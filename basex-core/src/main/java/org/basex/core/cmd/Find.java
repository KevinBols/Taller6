package org.basex.core.cmd;

import static org.basex.core.Text.*;
import static org.basex.util.Token.*;

import org.basex.core.*;
import org.basex.core.locks.*;
import org.basex.query.expr.path.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Evaluates the 'find' command and processes a simplified request as XQuery.
 *
 * @author BaseX Team 2005-22, BSD License
 * @author Christian Gruen
 */
public final class Find extends AQuery {
  /** Start from root node. */
  private final boolean root;

  /**
   * Default constructor.
   * @param query simplified query
   */
  public Find(final String query) {
    this(query, false);
  }

  /**
   * Default constructor.
   * @param query simplified query
   * @param rt start from root node
   */
  public Find(final String query, final boolean rt) {
    super(true, query);
    root = rt;
  }

  @Override
  protected boolean run() {
    final String query = query(args[0], context, root);
    final boolean ok = query(query);
    final StringBuilder sb = new StringBuilder();
    if(options.get(MainOptions.QUERYINFO)) {
      sb.append(NL).append(QUERY_CC).append(NL).append(query).append(NL);
    }
    sb.append(info());
    error(sb.toString());
    return ok;
  }

  @Override
  public boolean updating(final Context ctx) {
    return updates(ctx, query(args[0], ctx, root));
  }

  @Override
  public void addLocks() {
    jc().locks.reads.add(Locking.CONTEXT);
  }

  /**
   * Returns a query for the specified search string.
   * @param search search string
   * @param ctx database context
   * @param root start from root node
   * @return query string
   */
  public static String query(final String search, final Context ctx, final boolean root) {
    // treat input as XQuery
    if(Strings.startsWith(search, '/')) return search;

    final boolean r = root || ctx.root();
    if(search.isEmpty()) return r ? "/" : ".";

    // parse user input
    final StringBuilder pre = new StringBuilder(), preds = new StringBuilder();
    for(String term : split(search.replace(" +", " "))) {
      if(term.startsWith("@=")) {
        preds.append("[@* = \"").append(term.substring(2)).append("\"]");
      } else if(Strings.startsWith(term, '=')) {
        preds.append("[text() = \"").append(term.substring(1)).append("\"]");
      } else if(Strings.startsWith(term, '~')) {
        preds.append("[text() contains text \"").append(term.substring(1));
        preds.append("\" using fuzzy]");
      } else if(Strings.startsWith(term, '@')) {
        if(term.length() == 1) continue;
        preds.append("[@* contains text \"").append(term.substring(1)).append("\"]");
        term = term.substring(1);
        // add valid name tests
        if(XMLToken.isName(token(term))) {
          pre.append(r ? "" : ".").append("//@").append(term).append(" | ");
        }
      } else {
        preds.append("[text() contains text \"").append(term).append("\"]");
        // add valid name tests
        if(XMLToken.isName(token(term))) {
          if(r) pre.append('/');
          pre.append(Axis.DESCENDANT).append("::*:").append(term).append(" | ");
        }
      }
    }
    if(pre.length() == 0 && preds.length() == 0) return root ? "/" : ".";

    // create final string
    return pre + (r ? "/" : "") + Axis.DESCENDANT_OR_SELF + "::*" + preds;
  }

  /**
   * Creates a query for the specified table search.
   * @param filter filter terms
   * @param cols filter columns
   * @param elem element flag
   * @param name name of root element
   * @param root root flag
   * @return query
   */
  public static String tableQuery(final StringList filter, final TokenList cols,
      final BoolList elem, final String name, final boolean root) {

    final TokenBuilder tb = new TokenBuilder();
    final int is = filter.size();
    for(int i = 0; i < is; ++i) {
      final String[] spl = split(filter.get(i));
      for(final String s : spl) {
        final byte[] term = trim(replace(token(s), '"', ' '));
        if(term.length == 0) continue;
        final boolean elm = elem.get(i);
        tb.add('[').add(elm ? ".//" : "@").add("*:").add(cols.get(i));

        if(term[0] == '<' || term[0] == '>') {
          tb.add(term[0]).addLong(sizeToLong(substring(term, 1)));
        } else {
          tb.add(" contains text \"").add(term).add('"');
        }
        tb.add(']');
      }
    }
    return tb.isEmpty() ? "/" : (root ? "/" : "") + Axis.DESCENDANT_OR_SELF + "::*:" + name + tb;
  }

  /**
   * Converts the token with a size unit suffix to a numeric value.
   * The suffixes "kb", "mb" and "gb" are considered in the calculation.
   * @param token token to be converted
   * @return long
   */
  private static long sizeToLong(final byte[] token) {
    int tl = token.length, f = 0;
    final int s1 = tl < 1 ? 0 : lc(token[tl - 1]), s2 = tl < 2 ? 0 : lc(token[tl - 2]);
    if(s1 == 'k') { tl -= 1; f = 10; }
    if(s1 == 'm') { tl -= 1; f = 20; }
    if(s1 == 'g') { tl -= 1; f = 30; }
    if(s1 == 'b' && s2 == 'k') { tl -= 2; f = 10; }
    if(s1 == 'b' && s2 == 'm') { tl -= 2; f = 20; }
    if(s1 == 'b' && s2 == 'g') { tl -= 2; f = 30; }
    final long i = toLong(token, 0, tl) << f;
    return i == Long.MIN_VALUE ? 0 : i;
  }

  /**
   * Splits the string and returns an array.
   * @param string string to be split
   * @return array
   */
  private static String[] split(final String string) {
    final int l = string.length();
    final String[] split = new String[l];

    int s = 0;
    char delim = 0;
    final StringBuilder sb = new StringBuilder();
    for(int i = 0; i < l; ++i) {
      final char c = string.charAt(i);
      if(delim == 0) {
        if(c == '\'' || c == '"') {
          delim = c;
        } else if(!XMLToken.isChar(c) && c != '@' && c != '=' && c != '<' && c != '>' && c != '~') {
          if(sb.length() != 0) {
            split[s++] = sb.toString();
            sb.setLength(0);
          }
        } else {
          sb.append(c);
        }
      } else if(c == delim) {
        delim = 0;
        if(sb.length() != 0) {
          split[s++] = sb.toString();
          sb.setLength(0);
        }
      } else if(c != '\'' && c != '"') {
        sb.append(c);
      }
    }
    if(sb.length() != 0) split[s++] = sb.toString();
    return Array.copyOf(split, s);
  }
}
