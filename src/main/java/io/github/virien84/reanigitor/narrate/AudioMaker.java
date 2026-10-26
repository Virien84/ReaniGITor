package io.github.virien84.reanigitor.narrate;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Path;

/** Turns an ElevenLabs request body into an audio file. */
public interface AudioMaker {

    /** Text to speech: writes {@code out}. */
    void speech(ObjectNode body, Path out) throws IOException;

    /** Text to sound effect: writes {@code out}. */
    void soundEffect(ObjectNode body, Path out) throws IOException;
}
