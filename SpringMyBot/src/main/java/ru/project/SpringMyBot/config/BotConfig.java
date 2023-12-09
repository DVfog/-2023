package ru.project.SpringMyBot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data //автоматические создает конструкторы для класса
@PropertySource("application.properties")
public class BotConfig {
    //сюда будет обращаться главный класс TelegramBot чтобы получить токен и имя
    //бота для предоставления бэкенду телеграмма
    @Value("&{bot.name}")
    String botName;

    @Value("${bot.token}")
    String token;

    @Value("${bot.owner}")
    Long ownerID;
}
