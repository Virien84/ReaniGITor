package io.github.virien84.reanigitor.wake;

import io.github.virien84.reanigitor.github.Remains;

/** One tombstone: what GitHub gave us and what the model wrote about it. */
public record Grave(Remains remains, Autopsy autopsy, Calaverita calaverita) {
}
