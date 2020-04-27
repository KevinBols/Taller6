package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import java.util.*;
import java.util.function.*;

import org.basex.query.*;
import org.basex.query.CompileContext.*;
import org.basex.query.expr.ft.*;
import org.basex.query.expr.path.*;
import org.basex.query.func.Function;
import org.basex.query.util.*;
import org.basex.query.util.list.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.ft.*;

/**
 * Abstract predicate expression, implemented by {@link Filter} and {@link Step}.
 *
 * @author BaseX Team 2005-20, BSD License
 * @author Christian Gruen
 */
public abstract class Preds extends Arr {
  /**
   * Constructor.
   * @param info input info
   * @param seqType sequence type
   * @param exprs predicates
   */
  protected Preds(final InputInfo info, final SeqType seqType, final Expr... exprs) {
    super(info, seqType, exprs);
  }

  @Override
  public Expr compile(final CompileContext cc) throws QueryException {
    type(cc.qc.focus.value, cc);

    final int pl = exprs.length;
    if(pl != 0) {
      cc.pushFocus(this);
      try {
        final QueryFocus focus = cc.qc.focus;
        final Value init = focus.value;
        for(int p = 0; p < pl; ++p) {
          try {
            exprs[p] = exprs[p].compile(cc);
          } catch(final QueryException ex) {
            // replace original expression with error
            exprs[p] = cc.error(ex, this);
          }
        }
        focus.value = init;
      } finally {
        cc.removeFocus();
      }
    }
    return optimize(cc);
  }

  /**
   * Assigns the expression type. Needs to be called before the predicates are compiled.
   * @param expr root expression
   * @param cc compilation context
   */
  protected abstract void type(Expr expr, CompileContext cc);

  /**
   * Adds an expression to the new expression list.
   * @param expr expression
   * @param list expression list
   * @param pos positional access flag
   * @param cc compilation context
   * @return this, or a previous expression, uses positional access
   */
  private boolean addUnique(final Expr expr, final ExprList list, final boolean pos,
      final CompileContext cc) {

    final boolean ps = pos || expr.seqType().mayBeNumber() || expr.has(Flag.POS);
    if(expr == Bln.TRUE) {
      // skip predicate that yields true
      cc.info(OPTREMOVE_X_X, expr, (Supplier<?>) this::description);
    } else if(ps || !list.contains(expr) || expr.has(Flag.NDT)) {
      list.add(expr);
    }
    return ps;
  }

  /**
   * Assigns the sequence type and result size.
   * @param root root expression
   * @return whether expression may yield results
   */
  private boolean exprType(final Expr root) {
    long max = root.size();
    boolean exact = max != -1;
    if(!exact) max = Long.MAX_VALUE;

    // check positional predicates
    for(final Expr expr : exprs) {
      if(Function.LAST.is(expr)) {
        // use minimum of old value and 1
        max = Math.min(max, 1);
      } else if(expr instanceof ItrPos) {
        final ItrPos pos = (ItrPos) expr;
        // subtract start position. example: ...[1 to 2][2]  ->  2  ->  1
        if(max != Long.MAX_VALUE) max = Math.max(0, max - pos.min + 1);
        // use minimum of old value and range. example: ...[1 to 5]  ->  5
        max = Math.min(max, pos.max - pos.min + 1);
      } else {
        // resulting size will be unknown for any other filter
        exact = false;
      }
    }

    // choose exact result size; if not available, work with occurrence indicator
    final long size = exact || max == 0 ? max : -1;
    final Occ occ = max > 1 ? root.seqType().occ.union(Occ.ZERO) : Occ.ZERO_ONE;
    exprType.assign(root.seqType().type, occ, size);
    return max > 0;
  }

  /**
   * Checks if the specified item matches the predicates.
   * @param item item to be checked
   * @param qc query context
   * @return result of check
   * @throws QueryException query exception
   */
  protected final boolean match(final Item item, final QueryContext qc) throws QueryException {
    // set context value and position
    final QueryFocus qf = qc.focus;
    final Value cv = qf.value;
    qf.value = item;
    try {
      double s = qc.scoring ? 0 : -1;
      for(final Expr expr : exprs) {
        final Item test = expr.test(qc, info);
        if(test == null) return false;
        if(s != -1) s += test.score();
      }
      if(s > 0) item.score(Scoring.avg(s, exprs.length));
      return true;
    } finally {
      qf.value = cv;
    }
  }

  /**
   * Optimizes all predicates.
   * @param cc compilation context
   * @param root root expression
   * @return {@code true} if expression may yield results
   * @throws QueryException query exception
   */
  protected final boolean optimize(final CompileContext cc, final Expr root) throws QueryException {
    // remember current context value (will be temporarily overwritten)
    cc.pushFocus(root);
    try {
      simplify(cc, root);

      final int el = exprs.length;
      final ExprList list = new ExprList(el);
      boolean pos = false;
      for(final Expr ex : exprs) {
        final Expr ebv = ex.simplifyFor(Simplify.EBV, cc);
        Expr expr = ebv;
        if(expr instanceof And) {
          if(!expr.has(Flag.POS)) {
            // replace AND expression with predicates (don't rewrite position tests)
            cc.info(OPTPRED_X, expr);
            final Expr[] ands = ((Arr) expr).exprs;
            final int al = ands.length;
            for(int a = 0; a < al; a++) {
              // wrap test with boolean() if the result is numeric
              expr = ands[a];
              if(expr.seqType().mayBeNumber()) expr = cc.function(Function.BOOLEAN, info, expr);
              if(a + 1 < al) pos = addUnique(expr, list, pos, cc);
            }
          }
        } else if(expr instanceof ANum) {
          expr = ItrPos.get(((ANum) expr).dbl(), info);
        } else if(expr instanceof Value) {
          expr = Bln.get(expr.ebv(cc.qc, info).bool(info));
        }

        // example: <a/>/.[1]  ->  <a/>/.[true()]
        // example: $child/..[2]  ->  $child/..[false()]
        if(root instanceof Step && expr instanceof ItrPos) {
          final Axis axis = ((Step) root).axis;
          if(axis == Axis.SELF || axis == Axis.PARENT) expr = Bln.get(((ItrPos) expr).min == 1);
        }

        // predicate will not yield any results
        if(expr == Bln.FALSE) return false;
        if(expr != ebv) cc.replaceWith(ex, expr);

        pos = addUnique(expr, list, pos, cc);
      }
      exprs = list.finish();
      mergeEbv(false, false, cc);

    } finally {
      cc.removeFocus();
    }

    // check result size
    return exprType(root);
  }

  /**
   * Simplifies the predicates.
   * @param cc compilation context
   * @param root root expression
   * @throws QueryException query exception
   */
  private void simplify(final CompileContext cc, final Expr root) throws QueryException {
    final ExprList list = new ExprList(exprs.length);
    final SeqType st = root.seqType();
    for(final Expr expr : exprs) {
      Expr ex = expr;
      if(ex instanceof ContextValue && st.type instanceof NodeType) {
        // E [ . ]  ->  E
        cc.info(OPTREMOVE_X_X, ex, (Supplier<?>) this::description);
        continue;
      }

      // comparisons
      if(ex instanceof CmpG || ex instanceof CmpV) {
        ex = ((Cmp) ex).optPred(root, cc);
      }

      // map operator
      if(ex instanceof SimpleMap) {
        // E [ . ! ... ]  ->  E [ ... ]
        // E [ E ! ... ]  ->  E [ ... ]
        final SimpleMap map = (SimpleMap) ex;
        final Expr[] mexprs = map.exprs;
        final Expr first = mexprs[0], second = mexprs[1];
        if((first instanceof ContextValue || root.equals(first) && root.isSimple() && st.one()) &&
            !second.has(Flag.POS)) {
          final int ml = mexprs.length;
          ex = ml == 2 ? second : SimpleMap.get(map.info, Arrays.copyOfRange(mexprs, 1, ml));
        }
      }

      // paths
      if(ex instanceof Path) {
        if(ex instanceof SingleIterPath) {
          final Step predStep = (Step) ((Path) ex).steps[0];
          if(predStep.axis == Axis.SELF && !predStep.positional()) {
            if(root instanceof Step && !positional()) {
              final Step rootStep = (Step) root;
              final Test test = rootStep.test.intersect(predStep.test);
              if(test != null) {
                // child::node() [ self:* ]  ->  child::*
                cc.info(OPTMERGE_X, predStep);
                rootStep.test = test;
                list.add(predStep.exprs);
                continue;
              }
            }
            if(predStep.test instanceof KindTest && predStep.exprs.length == 0 &&
                st.type.instanceOf(predStep.test.type)) {
              // <a/> [ self:* ]  ->  <a/>
              cc.info(OPTREMOVE_X_X, ex, (Supplier<?>) this::description);
              continue;
            }
          }
        }

        // E [ . / ... ]  ->  E [ ... ]
        // E [ E / ... ]  ->  E [ ... ]
        final Path path = (Path) ex;
        final Expr first = path.root;
        if(st.type instanceof NodeType && (first instanceof ContextValue ||
            root.equals(first) && root.isSimple() && st.one())) {
          ex = Path.get(path.info, null, path.steps);
        }
      }

      // inline root item (ignore nodes)
      // 1[. = 1]  ->  1[1 = 1]
      if(root instanceof Item && !(st.type instanceof NodeType)) {
        final Expr inlined = ex.inline(null, root, cc);
        if(inlined != null) ex = inlined;
      }

      list.add(cc.replaceWith(expr, ex));
    }
    exprs = list.finish();
  }

  /**
   * Optimizes the predicates for boolean evaluation.
   * Drops solitary context values, flattens nested predicates.
   * @param root root expression
   * @param cc compilation context
   * @return expression
   * @throws QueryException query exception
   */
  public final Expr simplifyEbv(final Expr root, final CompileContext cc) throws QueryException {
    // only single predicate can be rewritten; root must yield nodes; no positional predicates
    final SeqType rst = root.seqType();
    final int el = exprs.length;
    if(!(rst.type instanceof NodeType) || el == 0 || positional()) return this;

    final Expr pred = exprs[el - 1];
    final QueryFunction<Expr, Expr> createRoot = r -> {
      return el == 1 ? r : Filter.get(info, r, Arrays.copyOfRange(exprs, 0, el - 1)).optimize(cc);
    };
    final QueryFunction<Expr, Expr> createExpr = e -> {
      return e instanceof ContextValue ? createRoot.apply(root) :
        e instanceof Path ? Path.get(info, createRoot.apply(root), e).optimize(cc) : null;
    };

    // rewrite to general comparison (right operand must not depend on context):
    // a[. = 'x']  ->  a = 'x'
    // a[text() = 'x']  ->  a/text() = 'x'
    if(pred instanceof CmpG) {
      // not applicable to value/node comparisons, as cardinality of expression might change
      final CmpG cmp = (CmpG) pred;
      final Expr expr1 = createExpr.apply(cmp.exprs[0]), expr2 = cmp.exprs[1];
      // right operand must not depend on context
      if(expr1 != null && !expr2.has(Flag.CTX)) {
        return new CmpG(expr1, expr2, cmp.op, cmp.coll, cmp.sc, cmp.info).optimize(cc);
      }
    }

    // rewrite to contains text expression (right operand must not depend on context):
    // a[. contains text 'x']  ->  a contains text 'x'
    // a[text() contains text 'x']  ->  a/text() contains text 'x'
    if(pred instanceof FTContains) {
      final FTContains cmp = (FTContains) pred;
      final Expr expr = createExpr.apply(cmp.expr);
      final FTExpr ftexpr = cmp.ftexpr;
      if(expr != null && !ftexpr.has(Flag.CTX)) {
        return new FTContains(expr, ftexpr, cmp.info).optimize(cc);
      }
    }

    // rewrite to path: root[path]  ->  root/path
    final Expr expr = createExpr.apply(pred);
    if(expr != null) return expr;

    // rewrite to simple map: $node[string()]  ->  $node ! string()
    if(rst.zeroOrOne()) return SimpleMap.get(info, createRoot.apply(root), pred).optimize(cc);

    return this;
  }

  /**
   * Checks if the specified expression returns an empty sequence or a deterministic numeric value.
   * @param expr expression
   * @return result of check
   */
  protected static boolean numeric(final Expr expr) {
    final SeqType st = expr.seqType();
    return st.type.isNumber() && st.zeroOrOne() && expr.isSimple();
  }

  /**
   * Checks if at least one of the predicates contains a positional access.
   * @return result of check
   */
  public boolean positional() {
    return positional(exprs);
  }

  /**
   * Checks if some of the specified expressions are positional.
   * @param exprs expressions
   * @return result of check
   */
  static boolean positional(final Expr[] exprs) {
    for(final Expr expr : exprs) {
      if(expr.seqType().mayBeNumber() || expr.has(Flag.POS)) return true;
    }
    return false;
  }

  @Override
  public boolean inlineable(final Var var) {
    for(final Expr expr : exprs) {
      if(expr.uses(var)) return false;
    }
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for(final Expr expr : exprs) sb.append('[').append(expr).append(']');
    return sb.toString();
  }
}
