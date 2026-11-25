package josue.com.bo.linde.util;

import android.view.View;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

/** Ayudas comunes de pantalla: bordes del sistema (edge-to-edge) y barra superior. */
public final class Pantalla {

    private Pantalla() {
    }

    /** Respeta la barra de estado, la de navegación y el teclado (Android 15+ dibuja de borde a borde). */
    public static void ajustarBordes(View raiz) {
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Pone la barra superior con su título; con flecha atrás si conAtras. */
    public static void barra(AppCompatActivity actividad, MaterialToolbar barra, @StringRes int titulo,
                             boolean conAtras) {
        actividad.setSupportActionBar(barra);
        if (actividad.getSupportActionBar() != null) {
            actividad.getSupportActionBar().setTitle(titulo);
            actividad.getSupportActionBar().setDisplayHomeAsUpEnabled(conAtras);
        }
        if (conAtras) barra.setNavigationOnClickListener(v -> actividad.getOnBackPressedDispatcher().onBackPressed());
    }
}
