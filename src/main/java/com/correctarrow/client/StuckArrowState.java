//#if MC >= 1.21.2
package com.correctarrow.client;

import org.jspecify.annotations.Nullable;

public interface StuckArrowState {
    ClientArrows.@Nullable Entry correctarrow$arrows();

    void correctarrow$setArrows(ClientArrows.@Nullable Entry arrows, float bodyYaw);

    float correctarrow$bodyYaw();

    ArrowPlacement.@Nullable Feet correctarrow$feet();

    void correctarrow$setFeet(ArrowPlacement.@Nullable Feet feet);

    boolean correctarrow$drawn();

    void correctarrow$setDrawn(boolean drawn);
}
//#endif
