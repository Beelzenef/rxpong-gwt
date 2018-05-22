package pong.client;

import static elemental2.dom.DomGlobal.window;
import static org.jboss.gwt.elemento.core.Elements.button;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.intendia.rxgwt2.elemento.RxElemento;
import elemental2.dom.HTMLButtonElement;
import org.jboss.gwt.elemento.core.Elements;
import org.jboss.gwt.elemento.core.EventType;

public class Pong implements EntryPoint {

    @Override public void onModuleLoad() {
        css().ensureInjected();

        final HTMLButtonElement btn;
        Elements.body().add(btn = button("Clickame para iniciar la JUEGACIÓN :)").get());

        RxElemento.fromEvent(btn, EventType.click).take(1).subscribe(ev -> window.alert("¡Bienvenid@ a RxPong!"));
    }

    private Resources.StylePong css() {
        return Resources.INSTANCE.style();
    }

    public interface Resources extends ClientBundle {
        Resources INSTANCE = GWT.create(Resources.class);

        @Source("style.gss") StylePong style();

        interface StylePong extends CssResource {
            String board();
        }
    }
}
