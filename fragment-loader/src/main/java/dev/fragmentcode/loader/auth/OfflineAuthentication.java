package dev.fragmentcode.loader.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Генерирует GameProfile без обращения к серверам Microsoft/Mojang.
 * Поведение полностью соответствует тому, как официальный launcher
 * генерирует uuid для offline-режима: детерминированный UUID версии 3
 * на основе имени игрока (одно и то же имя -> один и тот же uuid каждый раз,
 * это важно для совместимости с серверами, которые сохраняют данные
 * игрока по uuid).
 *
 * Подходит для:
 *   - одиночной игры
 *   - серверов с online-mode=false
 *
 * НЕ подходит для обычных серверов с включённой online-авторизацией -
 * там нужен настоящий Microsoft access token (см. будущий модуль fragment-auth).
 */
public final class OfflineAuthentication {

    public GameProfile createProfile(String username) {

        UUID offlineUuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
        );

        // accessToken не проверяется в offline-режиме, но должен быть
        // непустой строкой - некоторые версии игры падают на null/пустом.
        String fakeAccessToken = "0";

        return new GameProfile(username, offlineUuid, fakeAccessToken);

    }

}
