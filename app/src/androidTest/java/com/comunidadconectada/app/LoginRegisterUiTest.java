package com.comunidadconectada.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.comunidadconectada.app.ui.LoginActivity;
import com.comunidadconectada.app.ui.RegisterActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** UI-01..UI-03 (Espresso). Requieren emulador/dispositivo y google-services.json. Sesión cerrada al iniciar. */
@RunWith(AndroidJUnit4.class)
public class LoginRegisterUiTest {

    @Rule public ActivityScenarioRule<LoginActivity> loginRule = new ActivityScenarioRule<>(LoginActivity.class);

    @Test public void loginConCamposVacios_muestraErrores() {
        onView(withId(R.id.btn_login)).perform(click());
        onView(withText("Ingresa tu correo")).check(matches(isDisplayed()));
        onView(withText("Ingresa una contraseña")).check(matches(isDisplayed()));
    }

    @Test public void loginConCorreoInvalido_muestraError() {
        onView(withId(R.id.et_email)).perform(replaceText("correo-malo"), closeSoftKeyboard());
        onView(withId(R.id.et_password)).perform(replaceText("123456"), closeSoftKeyboard());
        onView(withId(R.id.btn_login)).perform(click());
        onView(withText("Correo no válido")).check(matches(isDisplayed()));
    }

    @Test public void registroConCamposVacios_muestraErrores_yNoRegistra() {
        androidx.test.core.app.ActivityScenario.launch(RegisterActivity.class);
        onView(withId(R.id.btn_register)).perform(click());
        onView(withText("Ingresa tu nombre")).check(matches(isDisplayed()));
        onView(withText("Ingresa el número de tu vivienda")).check(matches(isDisplayed()));
    }
}
