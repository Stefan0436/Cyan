package org.asf.cyan.api.config.gitmoduletest;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Comment;

@Comment("Git repository configuration.")
@Comment("This file configures the projects known to the server.")
@Comment("It also configures contributers and access rules.")
//These classes were taken from the git-support module because a bug was found that could not be reproduced
public class GitConfiguration extends Configuration<GitConfiguration> {

	public GitConfiguration(String baseDir) {
		super(baseDir);
	}

	public GitConfiguration() {
		super(".");
	}

	@Override
	public String filename() {
		return "repositories.ccfg";
	}

	@Override
	public String folder() {
		return "git-projects";
	}

	@Comment("Default access rules, applies to all repositories unless changed")
	public GitAccessConfig defaultAccess = new GitAccessConfig();

	@Comment("Map of git repositories, you can add repositories by calling the GitSupport jar from java -jar")
	public HashMap<String, GitRepositoryConfig> repositories = new HashMap<String, GitRepositoryConfig>();

}
