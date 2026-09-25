package com.drunkencod.spice_road.spice;

/**
 * Describes how a mature Spice Plant is actually harvested, independent of
 * its {@link SourceType} (growth shape). A single {@link SourceType} can
 * combine with more than one {@code HarvestAction} across different spices
 * (e.g. {@code TREE} spices are harvested by strip, pick, or shear depending
 * on the individual spice/harvested part).
 */
public enum HarvestAction {

    STRIP,
    PICK,
    SHEAR,
    BREAK
}
