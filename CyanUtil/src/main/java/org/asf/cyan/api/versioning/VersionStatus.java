package org.asf.cyan.api.versioning;

public enum VersionStatus {
	LATEST, LATEST_PREVIEW, LATEST_ALPHA, LATEST_BETA, OUTDATED, OUTDATED_PREVIEW, OUTDATED_BETA, OUTDATED_ALPHA,
	UNSUPPORTED, LTS, UNKNOWN;

	@Override
	public String toString() {
		switch (this) {
		case LATEST_PREVIEW:
			return "Latest PREVIEW";
		case LATEST_ALPHA:
			return "Latest ALPHA";
		case LATEST_BETA:
			return "Latest BETA";
		case OUTDATED_PREVIEW:
			return "Outdated PREVIEW";
		case OUTDATED_BETA:
			return "Outdated BETA";
		case OUTDATED_ALPHA:
			return "Outdated ALPHA";
		default:
			return name().replace("_", " ");			
		}
	}
}
