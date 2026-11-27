package emu.grasscutter.game.activity.leylinechallenge;

import emu.grasscutter.game.activity.*;
import emu.grasscutter.game.props.ActivityType;
import emu.grasscutter.net.proto.*;

@GameActivity(ActivityType.NEW_ACTIVITY_LEY_LINE_CHALLENGE)
public final class LeyLineChallengeActivityHandler extends ActivityHandler {
    @Override
    public void onProtoBuild(PlayerActivityData data, ActivityInfoOuterClass.ActivityInfo.Builder info) {
        var detail = LeyLineChallengeDetailInfo._LeyLineChallengeDetailInfo.newBuilder()
                .setPIJLAHENJEP(1).setOFBDHIJICPL(true).setLBCGNOOJDIC(true)
                .setLEEHFFLICIK(System.currentTimeMillis());
        for (int difficulty = 1; difficulty <= 6; difficulty++) {
            detail.addDifficultyInfoList(MKADIBABGKHOuterClass.MKADIBABGKH.newBuilder()
                    .setDifficulty(difficulty).build());
        }
        info.setLeyLineChallengeDetailInfo(detail);
    }

    @Override
    public void onInitPlayerActivityData(PlayerActivityData data) {}
}
