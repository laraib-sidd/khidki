package dev.laraib.khidki.domain.session

import dev.laraib.khidki.domain.model.ConfigurationId
import java.util.UUID

object TimedSessionDefaults {
    val PLACEHOLDER_CONFIG_ID: ConfigurationId =
        ConfigurationId(UUID.fromString("00000000-0000-4000-8000-000000000001"))

    const val LABEL: String = "Forwarding window"
}
