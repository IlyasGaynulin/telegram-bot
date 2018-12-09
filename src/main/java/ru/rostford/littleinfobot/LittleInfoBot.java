package ru.rostford.littleinfobot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rostford.littleinfobot.entity.Person;
import ru.rostford.littleinfobot.entity.TempPerson;
import ru.rostford.littleinfobot.entity.User;
import ru.rostford.littleinfobot.repository.PersonRepository;
import ru.rostford.littleinfobot.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Component
public class LittleInfoBot extends TelegramLongPollingBot {
    @Autowired
    PersonRepository personRepository;
    @Autowired
    UserRepository userRepository;
    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.username}")
    private String botUsername;

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            User currentUser = userRepository.findUserByUserId(message.getChatId());
            String text = message.getText();

            if(currentUser == null) {
                currentUser = new User();
                currentUser.setUserId(message.getChatId());
                userRepository.save(currentUser);
            }

            switch(text) {
                case("/help"): {
                    userRepository.setCurrentAction(User.CurrentAction.NOTHING, currentUser.getUserId());
                    sendSimpleMessage(
                            "Здравствуйте, я бот, который выдаёт имеющуюся у меня информацию о человеке, введите ФИО человека и я выдам информацию о нём",
                            currentUser.getUserId()
                    );
                    return;
                }
                case("/start"): {
                    userRepository.setCurrentAction(User.CurrentAction.NOTHING, currentUser.getUserId());
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(message.getChatId());
                    sendMessage.setText("Выберите нужную команду");
                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> keyboard = new ArrayList<>();
                    KeyboardRow row = new KeyboardRow();
                    row.add("/help");
                    row.add("/add");
                    row.add("/abort");
                    keyboard.add(row);
                    keyboardMarkup.setKeyboard(keyboard);
                    sendMessage.setReplyMarkup(keyboardMarkup);
                    try {
                        execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                case("/edit"): {
                    if(!currentUser.isModerator()) {
                        sendSimpleMessage("У вас недостаточно прав для изменения записей", currentUser.getUserId());
                        return;
                    }
                    switch (currentUser.getLastFoundedId()) {
                        case(0): {
                            sendSimpleMessage(
                                    "Вы не нашли ещё ни одного пользователя, которого я бы мог поменять. Введите его ФИО.",
                                    currentUser.getUserId()
                            );
                            return;
                        }
                        case(-1): {
                            sendSimpleMessage(
                                    "В последнем поиске было найдено более одного пользователя, я не могу понять, кого мне менять.",
                                    currentUser.getUserId()
                            );
                            return;
                        }
                        default: {
                            sendSimpleMessage(
                                    "Введите новую информацию о пользователе, оно заменится заместо старого " +
                                            "(для отмены операции введите команду /abort)",
                                    currentUser.getUserId()
                            );
                            currentUser.setCurrentAction(User.CurrentAction.EDIT_LAST_USER);
                            userRepository.save(currentUser);
                            return;
                        }
                    }
                }
                case("/add"): {
                    sendSimpleMessage("Добавление новой записи (для отмены операции введите или выберите /abort)", currentUser.getUserId());
                    userRepository.setCurrentAction(User.CurrentAction.ADD_LAST_NAME, currentUser.getUserId());
                    sendSimpleMessage("Введите фамилию", currentUser.getUserId());
                    return;
                }
                case("/delete"): {
                    sendSimpleMessage("Пока эта функция не реализована", currentUser.getUserId());
                    return;
                }
                case("/abort"): {
                    sendSimpleMessage("Сброс несохранённых изменений", currentUser.getUserId());
                    userRepository.setCurrentAction(User.CurrentAction.NOTHING, currentUser.getUserId());
                    return;
                }
                default: {
                    switch (currentUser.getCurrentAction()) {
                        case(User.CurrentAction.NOTHING): {
                            findPersonByFullName(text, currentUser);
                            return;
                        }
                        case(User.CurrentAction.EDIT_LAST_USER): {
                            editUser(text, currentUser);
                            return;
                        }
                        case(User.CurrentAction.ADD_LAST_NAME):
                        case(User.CurrentAction.ADD_FIRST_NAME):
                        case(User.CurrentAction.ADD_MIDDLE_NAME):
                        case(User.CurrentAction.ADD_INFO): {
                            addUserProcess(text, currentUser);
                            return;
                        }
                    }
                }
            }
        }
    }

    public Person findPersonByFullName(String text, User currentUser) {
        Person foundedPerson = personRepository.findByFullInfo(text.replaceAll(" ", ""));
        if(foundedPerson == null) {
            sendSimpleMessage("Человек не найден в базе", currentUser.getUserId());
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(currentUser.getUserId());
            sendMessage.setText(
                    "Найден человек:\n" +
                    foundedPerson.toString() + "\n" +
                    "Идентификатор: " + foundedPerson.getId() + "\n\n" +
                    foundedPerson.getInfo()
            );
            currentUser.setLastFoundedId(foundedPerson.getId());
            userRepository.save(currentUser);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return foundedPerson;
    }

    public void addUserProcess(String text, User currentUser) {
        switch (currentUser.getCurrentAction()) {
            case(User.CurrentAction.ADD_LAST_NAME): {
                sendSimpleMessage("Введите имя человека", currentUser.getUserId());
                TempPerson tempPerson = new TempPerson();
                tempPerson.setLastName(text);
                currentUser.setTempPerson(tempPerson);
                currentUser.setCurrentAction(User.CurrentAction.ADD_FIRST_NAME);
                userRepository.saveAndFlush(currentUser);
                return;
            }
            case(User.CurrentAction.ADD_FIRST_NAME): {
                sendSimpleMessage("Теперь введите отчество", currentUser.getUserId());
                TempPerson tempPerson = currentUser.getTempPerson();
                tempPerson.setFirstName(text);
                currentUser.setTempPerson(tempPerson);
                currentUser.setCurrentAction(User.CurrentAction.ADD_MIDDLE_NAME);
                userRepository.saveAndFlush(currentUser);
                return;
            }
            case(User.CurrentAction.ADD_MIDDLE_NAME): {
                sendSimpleMessage("Введите информацию о нём", currentUser.getUserId());
                TempPerson tempPerson = currentUser.getTempPerson();
                tempPerson.setMiddleName(text);
                currentUser.setTempPerson(tempPerson);
                currentUser.setCurrentAction(User.CurrentAction.ADD_INFO);
                userRepository.saveAndFlush(currentUser);
                return;
            }
            case(User.CurrentAction.ADD_INFO): {
                TempPerson tempPerson = currentUser.getTempPerson();
                tempPerson.setInfo(text);
                currentUser.setTempPerson(null);
                personRepository.save(tempPerson.toPerson());
                currentUser.setCurrentAction(User.CurrentAction.NOTHING);
                userRepository.saveAndFlush(currentUser);
                sendSimpleMessage("Информация успешно сохранена", currentUser.getUserId());
                return;
            }
        }
    }

    public void editUser(String text, User currentUser) {
            Person editPerson = personRepository.findById(currentUser.getLastFoundedId());
            editPerson.setInfo(text);
            personRepository.save(editPerson);
            sendSimpleMessage("Информация о пользователе обновлена", currentUser.getUserId());
            userRepository.setCurrentAction(User.CurrentAction.NOTHING, currentUser.getUserId());
            return;
    }

    public void sendSimpleMessage(String text, long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }


}
