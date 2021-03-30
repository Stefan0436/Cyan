package org.asf.cyan.api.config.gitmoduletest;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Comment;

// These classes were taken from the git-support module because a bug was found that could not be reproduced
public class GitAccessConfig extends Configuration<GitAccessConfig> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}
	
	public GitAccessConfig() {
		authorizationRules.put("pull", false);
		authorizationRules.put("push", true);
	}
	
	@Comment("The credential group used for the repository security")
	public String accessGroup = "git";

	@Comment("Default authorization rules, true means the method needs authorization, false means it doesn't")
	public HashMap<String, Boolean> authorizationRules = new HashMap<String, Boolean>();
	
}
