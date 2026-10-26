package io.github.virien84.reanigitor.wake;

import java.util.List;

/** Stock texts for when the model does not answer properly, so every repo still gets a grave. */
final class Fallbacks {

    private Fallbacks() {
    }

    static Autopsy autopsy() {
        return new Autopsy(
                "Cause of death unknown: even the coroner's crystal ball refused to look.",
                "Gone, but not git-ignored.",
                List.of("Light a candle and clone the repo.",
                        "Run the build and write down everything that breaks.",
                        "Push one commit, any commit, to prove there is still a pulse."),
                true);
    }

    static Calaverita calaverita(Lang lang) {
        if (lang == Lang.ES) {
            return new Calaverita("Llegó la Huesuda", List.of(
                    "Vino la Huesuda a buscarte,",
                    "repo de commits olvidados;",
                    "nadie quiso ya actualizarte,",
                    "tus issues quedaron colgados.",
                    "«Vente», te dijo riendo,",
                    "«que nadie te viene a clonar»;",
                    "y con su guadaña sonriendo",
                    "te llevó al panteón a descansar."), lang, true);
        }
        return new Calaverita("The Catrina came by", List.of(
                "The Catrina came by in the night,",
                "she searched every branch for a sign;",
                "no commit, no issue, no light,",
                "just a README frozen in time.",
                "\"Come along,\" she said with a grin,",
                "\"your stars cannot keep you alive;",
                "your maintainer stopped logging in,",
                "but my graveyard is where you will thrive.\""), lang, true);
    }
}
