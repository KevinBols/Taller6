package org.basex.query.expr;

import static org.basex.query.QueryError.*;
import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.util.list.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Checks the argument expression's result type.
 *
 * @author BaseX Team 2005-18, BSD License
 * @author Leo Woerteler
 */
public final class TypeCheck extends Single {
  /** Static context. */
  private final StaticContext sc;
  /** Flag for function conversion. */
  public final boolean promote;

  /**
   * Constructor.
   * @param sc static context
   * @param info input info
   * @param expr expression to be promoted
   * @param seqType type to promote to
   * @param promote flag for function promotion
   */
  public TypeCheck(final StaticContext sc, final InputInfo info, final Expr expr,
      final SeqType seqType, final boolean promote) {
    super(info, expr, seqType);
    this.sc = sc;
    this.promote = promote;
  }

  @Override
  public Expr compile(final CompileContext cc) throws QueryException {
    return super.compile(cc).optimize(cc);
  }

  @Override
  public Expr optimize(final CompileContext cc) throws QueryException {
    final SeqType at = expr.seqType(), st = seqType();

    // return type is already correct
    if(at.instanceOf(st)) {
      cc.info(OPTTYPE_X, st + " -> " + expr);
      return expr;
    }

    // function item coercion
    if(expr instanceof FuncItem && st.type instanceof FuncType) {
      if(!st.occ.check(1)) throw typeError(expr, st, null, info);
      final FuncItem fi = (FuncItem) expr;
      return cc.replaceWith(this, fi.coerceTo((FuncType) st.type, cc.qc, info, true));
    }

    // we can type check immediately
    if(expr instanceof Value) return cc.preEval(this);

    // check at each call
    if(at.type.instanceOf(st.type)) {
      final Occ occ = at.occ.intersect(st.occ);
      if(occ == null) throw typeError(expr, st, null, info);
    }

    final Expr opt = expr.typeCheck(this, cc);
    if(opt != null) {
      cc.info(OPTTYPE_X, st + " -> " + opt);
      return opt;
    }

    return this;
  }

  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    final SeqType st = seqType();
    final Iter iter = expr.iter(qc);

    return new Iter() {
      final ItemList items = new ItemList();
      int c, i;

      @Override
      public Item next() throws QueryException {
        while(c == items.size()) {
          items.size(0);
          c = 0;

          final Item item = qc.next(iter);
          if(item == null || st.instance(item)) {
            items.add(item);
          } else if(promote) {
            st.promote(item, null, items, qc, sc, info, false);
          } else {
            throw typeError(expr, st, null, info);
          }
        }

        final Item item = items.get(c);
        items.set(c++, null);
        if(item == null && i < st.occ.min || i > st.occ.max) throw typeError(expr, st, null, info);
        i++;
        return item;
      }
    };
  }

  @Override
  public Value value(final QueryContext qc) throws QueryException {
    final Value value = expr.value(qc);
    final SeqType st = seqType();
    if(st.instance(value)) return value;
    if(promote) return st.promote(value, null, qc, sc, info, false);
    throw typeError(value, st, null, info);
  }

  /**
   * Checks if this type check is redundant if the result is bound to the given variable.
   * @param var variable
   * @return result of check
   */
  public boolean isRedundant(final Var var) {
    return (!promote || var.promotes()) && var.declaredType().instanceOf(seqType());
  }

  /**
   * Creates an expression that checks the given expression's return type.
   * @param ex expression to check
   * @param cc compilation context
   * @return the resulting expression
   * @throws QueryException query exception
   */
  public Expr check(final Expr ex, final CompileContext cc) throws QueryException {
    final SeqType at = ex.seqType(), st = seqType();
    return at.instanceOf(st) ? ex : new TypeCheck(sc, info, ex, st, promote).optimize(cc);
  }

  @Override
  public boolean equals(final Object obj) {
    if(this == obj) return true;
    if(!(obj instanceof TypeCheck)) return false;
    final TypeCheck tc = (TypeCheck) obj;
    return seqType().eq(tc.seqType()) && promote == tc.promote && super.equals(obj);
  }

  @Override
  public Expr copy(final CompileContext cc, final IntObjMap<Var> vm) {
    return new TypeCheck(sc, info, expr.copy(cc, vm), seqType(), promote);
  }

  @Override
  public void plan(final FElem plan) {
    final FElem elem = planElem(AS, seqType());
    if(promote) elem.add(planAttr(PROMOTE, true));
    addPlan(plan, elem, expr);
  }

  @Override
  public String toString() {
    return "((: " + seqType() + ", " + promote + " :) " + expr + ')';
  }
}
