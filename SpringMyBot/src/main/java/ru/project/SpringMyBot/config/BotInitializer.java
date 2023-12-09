package ru.project.SpringMyBot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.project.SpringMyBot.service.TelegramBot;

@Slf4j //Аннотация Lombok для автоматической генерации логгера.
@Component //Аннотация Spring, помечающая этот класс как компонент, чтобы Spring мог управлять им.
public class BotInitializer {

    @Autowired //Аннотация, указывающая Spring внедрить зависимость автоматически.
    TelegramBot bot;

    @EventListener({ContextRefreshedEvent.class})
    //Метод регистрирует вашего бота в Telegram API при старте приложения.
    public void init () throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            telegramBotsApi.registerBot(bot);
        }
        catch (TelegramApiException e){
            log.error("Error: " + e.getMessage());
        }
    }
}

