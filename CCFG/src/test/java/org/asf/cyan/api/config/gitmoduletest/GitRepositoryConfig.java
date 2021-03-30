package org.asf.cyan.api.config.gitmoduletest;

import org.asf.cyan.api.config.annotations.Comment;

//These classes were taken from the git-support module because a bug was found that could not be reproduced
public class GitRepositoryConfig extends GitAccessConfig {
	
	@Comment("The autor name")
	public String author = "none";

}
