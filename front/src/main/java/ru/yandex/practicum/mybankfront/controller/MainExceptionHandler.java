package ru.yandex.practicum.mybankfront.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.yandex.practicum.mybankfront.service.MainPageModelFactory;

import java.util.List;
import java.util.UUID;

/**
 * Превращает ошибки разбора параметров формы в сообщение на странице main.html
 * вместо страницы ошибки 400.
 */
@ControllerAdvice(assignableTypes = MainController.class)
public class MainExceptionHandler {

    private final MainPageModelFactory modelFactory;

    public MainExceptionHandler(MainPageModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handleMissingParameter(MissingServletRequestParameterException exception,
                                         Model model) {
        String message = switch (exception.getParameterName()) {
            case "name" -> "Фамилия и имя обязательны для заполнения";
            case "birthdate" -> "Дата рождения обязательна для заполнения";
            case "value" -> "Сумма обязательна для заполнения";
            case "login" -> "Необходимо выбрать получателя";
            case "action" -> "Не выбрано действие";
            default -> "Все поля должны быть заполнены";
        };
        return renderError(model, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException exception, Model model) {
        String message = switch (String.valueOf(exception.getName())) {
            case "value" -> "Сумма должна быть числом";
            case "birthdate" -> "Дата рождения должна быть в формате ГГГГ-ММ-ДД";
            case "action" -> "Некорректное действие с деньгами";
            default -> "Введены некорректные данные";
        };
        return renderError(model, message);
    }

    private String renderError(Model model, String message) {
        modelFactory.fill(model, List.of(message), null, UUID.randomUUID(), UUID.randomUUID());
        return "main";
    }
}
