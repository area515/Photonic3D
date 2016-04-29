package org.area515.util;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.rewrite.handler.RegexRule;

public class RedirectRegexRule extends RegexRule {
	private String _replacement;
	private String _query;
	private boolean _queryGroup;

	public RedirectRegexRule() {
		_handling = false;
		_terminating = false;
	}

	public void setReplacement(String replacement) {
		String[] split = replacement.split("\\?", 2);
		_replacement = split[0];
		_query = (split.length == 2 ? split[1] : null);
		_queryGroup = ((_query != null) && (_query.contains("$Q")));
	}

	public String apply(String target, HttpServletRequest request, HttpServletResponse response, Matcher matcher) throws IOException {
		target = _replacement;
		String query = _query;
		for (int g = 1; g <= matcher.groupCount(); g++) {
			String group = matcher.group(g);
			if (group == null) {
				group = "";
			} else
				group = Matcher.quoteReplacement(group);
			target = target.replaceAll("\\$" + g, group);
			if (query != null) {
				query = query.replaceAll("\\$" + g, group);
			}
		}
		
		if (query != null) {
			if (_queryGroup) {
				query = query.replace("$Q", request.getQueryString() == null ? "" : request.getQueryString());
				target += "?" + query;
			}
		}
		
	    response.sendRedirect(response.encodeRedirectURL(target));
		return target;
	}

	public String toString() {
		return super.toString() + "[" + _replacement + "]";
	}
}