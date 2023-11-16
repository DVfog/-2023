package ru.project.SpringMyBot.service;


import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.project.SpringMyBot.config.BotConfig;
import ru.project.SpringMyBot.model.User;
import ru.project.SpringMyBot.model.UserRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component //позволяет автоматически создать экземпляр Спрингу
public class TelegramBot extends TelegramLongPollingBot //расширение класс позволяющее общаться юоту с телеграммом
    {

    @Autowired
    private UserRepository userRepository;
    final BotConfig config;
    static final String HELP_TEXT = "Этот бот создан для КУРСОВОЙ РАБОТЫ.\n\n" +
            "Вы можете выполнять команды из главного меню слева или набрав команду:\n\n" +
            "Введите /start чтобы начать работу.\n";

    //Конструктор
    public TelegramBot(BotConfig config) {
        this.config = config;

        //Меню бота в нижнем левом углу
        List<BotCommand> listofCommand= new ArrayList<>();
        listofCommand.add (new BotCommand("/start","Приветсвие бота"));
        listofCommand.add (new BotCommand("/mydata", "Показать мою информацию"));
        listofCommand.add (new BotCommand("/help","Информация по использованию бота"));
        listofCommand.add (new BotCommand("/settings", "Настройки"));
        try{
            this.execute(new SetMyCommands(listofCommand,new BotCommandScopeDefault(),null));
        }
        catch (TelegramApiException e){
        }

        }



    @Override
    //бот передает телеграмму свое имя
    public String getBotUsername() { return config.getBotName(); }

    @Override
    //бот передает телграмму свое имя
    public String getBotToken() { return config.getToken(); }

    @Override
    //Главный, центтральный метод всего приложение,
    //тут происхдит обработка входных данных и возвращает ответ.
    public void onUpdateReceived(Update update) {
        //Проверка ботом чата на начличие новых сообщений
        if(update.hasMessage() && update.getMessage().hasText()){
            String messegeText = update.getMessage().getText();
            long chatID = update.getMessage().getChatId();
            //Команды для бота
            switch (messegeText) {
                //При вызове пользователем этой команды, бот вызывает метод проверки регистрации пользователя в бд.
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatID, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    sendMessage(chatID, HELP_TEXT);
                    break;
                case "Люблю Улика":
                    sendMessage(chatID,"Улик тоже тебя любит");
                    break;
                default: //ответ бота на не определённые комнады
                    sendMessage(chatID, "Sorry");

            }
        }
    }
    //Тут бот сначала сверяет пользователя в базе данных и если не находит зарегистрированного пользователя, то регистрирует его
    private void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty()){
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();
            user.setChatID(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);

        }
    }

    //Метод приветсвия бота
    private void startCommandReceived(Long chatID, String name) {
        String answer = "Привет," + name;
        sendMessage(chatID, answer);

    }
    private void sendMessage(long chatID, @NonNull String textToSend){
        SendMessage message = new SendMessage ();
        message.setChatId(String.valueOf(chatID));
        message.setText(textToSend);

        //Создание экранной клавиатуры бота.
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Квесты");
        row.add("Патроны");

        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Броня");
        row.add("Сборки оружия");
        row.add("Новости");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        try{
            execute(message);
        }
        catch (TelegramApiException e){
        }
    }
}
