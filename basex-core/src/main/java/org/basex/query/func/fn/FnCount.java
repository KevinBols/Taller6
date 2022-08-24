package org.basex.query.func.fn;

import static org.basex.query.func.Function.*;

import org.basex.query.*;
import org.basex.query.CompileContext.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-22, BSD License
 * @author Christian Gruen
 */
public final class FnCount extends StandardFunc {
  @Override
  public Int item(final QueryContext qc, final InputInfo ii) throws QueryException {
    // iterative access: if the iterator size is unknown, iterate through all results
    final Iter input = exprs[0].iter(qc);
    long size = input.size();
    if(size == -1) {
      do ++size; while(qc.next(input) != null);
    }
    return Int.get(size);
  }

  @Override
  protected void simplifyArgs(final CompileContext cc) throws QueryException {
    exprs[0] = exprs[0].simplifyFor(Simplify.COUNT, cc);
  }

  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    // return static result size
    final Expr input = exprs[0];
    final long size = input.size();
    if(size >= 0 && !input.has(Flag.NDT)) return Int.get(size);

    // count(map:keys(E))  ->  map:size(E)
    if(_MAP_KEYS.is(input))
      return cc.function(_MAP_SIZE, info, input.args());
    // count(util:array-members(E))  ->  array:size(E)
    if(_UTIL_ARRAY_MEMBERS.is(input))
      return cc.function(_ARRAY_SIZE, info, input.args());
    // count(string-to-codepoints(E))  ->  string-length(E)
    // count(characters(E))  ->  string-length(E)
    if(STRING_TO_CODEPOINTS.is(input) || CHARACTERS.is(input))
      return cc.function(STRING_LENGTH, info, input.args());

    return embed(cc, true);
  }

  @Override
  public Expr simplifyFor(final Simplify mode, final CompileContext cc) throws QueryException {
    if(mode == Simplify.EBV) {
      // if(count(nodes))  ->  if(nodes)
      // if(count(items))  ->  if(exists(items))
      final Expr input = exprs[0];
      return cc.simplify(this, input.seqType().type instanceof NodeType ? input :
        cc.function(EXISTS, info, exprs));
    }
    return this;
  }
}
