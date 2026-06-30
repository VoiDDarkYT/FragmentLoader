package dev.fragmentcode.loader.auth;

import java.util.UUID;

/**
 * Минимальные данные об игроке, нужные для подстановки в game-аргументы
 * запуска (${auth_player_name}, ${auth_uuid}, ${auth_access_token} и т.д.).
 *
 * Для offline-режима accessToken - произвольная непустая строка (игра
 * её не проверяет при offline-запуске одиночной игры или подключении
 * к серверу с online-mode=false).
 *
 * Позже appendable: модуль fragment-auth будет создавать GameProfile
 * с настоящими данными после прохождения Microsoft OAuth - вызывающий
 * код (FragmentLauncher) не должен зависеть от того, как профиль получен.
 */
public final class GameProfile {

    private final String username;
    private final UUID uuid;
    private final String accessToken;

    public GameProfile(String username, UUID uuid, String accessToken) {
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getAccessToken() {
        return accessToken;
    }

}
