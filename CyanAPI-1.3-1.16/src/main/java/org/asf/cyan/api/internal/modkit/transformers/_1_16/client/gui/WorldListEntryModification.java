package org.asf.cyan.api.internal.modkit.transformers._1_16.client.gui;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.asf.cyan.api.internal.modkit.components._1_16.common.LoadUtil;
import org.asf.cyan.api.internal.modkit.transformers._1_16.common.world.storage.LevelModDataReader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.gui.screens.worldselection.WorldSelectionList$WorldListEntry")
public class WorldListEntryModification {

	private final LevelSummary summary = null;
	private final SelectWorldScreen screen = null;
	private final Minecraft minecraft = null;

	@Reflect
	public void loadWorld() {
	}

	public static boolean checkWorldJoin(LevelSummary summary, SelectWorldScreen screen, Minecraft minecraft,
			Runnable callback) {
		return LoadUtil.checkWorldJoin((LevelModDataReader) summary, summary.getWorldVersionName().getString(), false,
				status -> {
					TranslatableComponent comp1 = new TranslatableComponent("modkit.worldbackup.question");
					TranslatableComponent comp2 = new TranslatableComponent("modkit.worldbackup.message", status);
					minecraft.setScreen(new BackupConfirmScreen(screen, (createBackup, eraseCache) -> {
						if (createBackup) {
							try {
								Throwable throwable = null;
								LevelStorageAccess acc = minecraft.getLevelSource().createAccess(summary.getLevelId());
								try {
									EditWorldScreen.makeBackupAndShowToast(acc);
								} catch (Throwable t) {
									throwable = t;
									throw t;
								} finally {
									try {
										if (acc != null)
											acc.close();
									} catch (Throwable t) {
										if (throwable != null) {
											throwable.addSuppressed(t);
										} else {
											throw t;
										}
									}
								}
							} catch (IOException e) {
								SystemToast.onWorldAccessFailure(minecraft, summary.getLevelId());
								LogManager.getLogger(WorldSelectionList.class)
										.error("Failed to backup level " + summary.getLevelId(), e);
							}
						}
						callback.run();
					}, comp1, comp2, false));
				});
	}

	@InjectAt(location = InjectLocation.HEAD, targetCall = "shouldBackup()", targetOwner = "net.minecraft.world.level.storage.LevelSummary")
	public void joinWorld() {
		if (checkWorldJoin(summary, screen, minecraft, () -> loadWorld()))
			return;
		return;
	}

}
