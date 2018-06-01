package pong.client;

import static elemental2.dom.DomGlobal.document;
import static io.reactivex.Observable.empty;
import static io.reactivex.Observable.just;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.NaN;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jboss.gwt.elemento.core.Elements.body;
import static org.jboss.gwt.elemento.core.EventType.keydown;
import static org.jboss.gwt.elemento.core.EventType.keyup;
import static org.jboss.gwt.elemento.core.Key.Spacebar;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.intendia.rxgwt2.elemento.RxElemento;
import elemental2.core.JsDate;
import elemental2.dom.CanvasRenderingContext2D;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.EventTarget;
import elemental2.dom.HTMLCanvasElement;
import elemental2.dom.HTMLParagraphElement;
import elemental2.dom.KeyboardEvent;
import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import jsinterop.base.Js;
import org.jboss.gwt.elemento.core.Elements;
import org.jboss.gwt.elemento.core.EventType;

public class Pong implements EntryPoint {

    @Override public void onModuleLoad() {
        css().ensureInjected();

        HTMLCanvasElement canvas = Elements.canvas().css(css().board()).asElement();
        HTMLParagraphElement debug = Elements.p().css(css().gamestatus()).asElement();

        canvas.width = 480;
        canvas.height = 600;
        body().add(canvas);
        body().add(debug);

        CanvasRenderingContext2D context = Js.cast(canvas.getContext("2d"));
        context.fillStyle = CanvasRenderingContext2D.FillStyleUnionType.of("orange");

        int PADDLEWIDTH = 50;
        int PADDLEHEIGHT = 15;

        int PADDLESPEED = 240;

        int BALLRADIUS = 5;
        int BALL_SPEED = 200;

        final String[] playerToWin = {"Player 2"};

        class XY {
            double x, y;

            public XY(double x, double y) {
                this.x = x;
                this.y = y;
            }
        }

        Runnable infoAuthor = () -> {
            context.textAlign = "center";
            context.font = "14px Courier New";
            context.fillText("RxPong GWT, coded with ☕ by Elena G", canvas.width / 2, canvas.height / 2 + 24);
            context.fillText("Remagined by Ignacio Baca for learning purposes,", canvas.width / 2, canvas.height / 2 + 40);
            context.fillText("based on Breakout by Manuel Wieser", canvas.width / 2, canvas.height / 2 + 56);

        };

        Runnable infoControls = () -> {
            context.textAlign = "center";
            context.font = "16px Courier New";
            context.fillText("press [A][D] or [J][L] to play, [space] to start", canvas.width / 2, canvas.height / 2);
        };

        Runnable infoTitle = () -> {
            context.textAlign = "center";
            context.font = "24px Courier New";
            context.fillText("RxPong (RxJava + GWT)", canvas.width / 2, canvas.height / 2 - 24);
        };

        Consumer<String> drawGameOver = text -> {
            context.clearRect(canvas.width / 4, canvas.height / 3, canvas.width / 2, canvas.height / 3);
            context.textAlign = "center";
            context.font = "24px Courier New";
            context.fillText(text, canvas.width / 2, canvas.height / 2);
            context.font = "16px Courier New";
            context.fillText("press any key to play again", canvas.width / 2, canvas.height / 2 + 24);
        };

        Consumer<String> drawState = text -> debug.textContent = text;

        DoubleFunction<DoubleConsumer> drawPaddle = y -> x -> {
            context.beginPath();
            context.rect(x - PADDLEWIDTH / 2, y - PADDLEHEIGHT, PADDLEWIDTH, PADDLEHEIGHT);
            context.fill();
            context.closePath();
        };

        DoubleConsumer drawPaddle1 = drawPaddle.apply(context.canvas.height);
        DoubleConsumer drawPaddle2 = drawPaddle.apply(PADDLEHEIGHT);

        Consumer<XY> drawBall = ball -> {
            context.beginPath();
            context.arc(ball.x, ball.y, BALLRADIUS, 0, Math.PI * 2);
            context.fill();
            context.closePath();
        };


        int TICKER_INTERVAL = 20;

        class Tick {
            double time;
            double delta;

            public Tick(double time, double delta) {
                this.time = time;
                this.delta = delta;
            }
        }

        Observable<KeyboardEvent> space$ = RxElemento.fromEvent(document, keydown).filter(Spacebar::match);
        Observable<Boolean> toggle$ = space$.scan(FALSE, (acc, n) -> !acc).filter(TRUE::equals);
        Observable<Tick> ticker$ = Observable.interval(TICKER_INTERVAL, MILLISECONDS)
                .join(toggle$, l -> empty(), r -> space$, (l, r) -> l)
                .map(n -> new Tick(JsDate.now(), NaN))
                .scan((previous, current) -> new Tick(current.time, (current.time - previous.time) / 1000))
                .skip(1);

        class BallState {
            final XY position;
            final XY direction;

            BallState(XY position, XY direction) {
                this.position = position;
                this.direction = direction;
            }
        }

        class State {
            Tick tick;
            double paddle1;
            double paddle2;
            BallState ball;

            public State copy() {
                State out = new State();
                out.tick = tick;
                out.paddle1 = paddle1;
                out.paddle2 = paddle2;
                out.ball = new BallState(ball.position, ball.direction);
                return out;
            }

            public boolean over() { return 0 > ball.position.y || ball.position.y > canvas.height; }

            @Override public String toString() {
                return "Game status: {" +
                        "\n ball speed -> " + BALL_SPEED +
                        "\n paddle position 1 -> " + (int)paddle1 +
                        "\n paddle position 2 -> " + (int)paddle2 +
                        "\n ball position -> " + (int)ball.position.x + ":" + (int)ball.position.y + "\n }";
            }
        }

        Callable<State> initialState = () -> {
            State out = new State();
            out.tick = new Tick(0, 0);
            out.paddle1 = canvas.width / 2.;
            out.paddle2 = canvas.width / 2.;
            out.ball = new BallState(new XY(canvas.width / 2, canvas.height / 2), new XY(1.2, 1.2));
            return out;
        };

        Function<Map<String, Integer>, Observable<Integer>> fromKey = (keys) -> fromEvent(document, keydown)
                .filter(ev -> keys.containsKey(ev.code))
                .switchMap(ev -> just(keys.get(ev.code))
                        .concatWith(fromEvent(document, keyup)
                                .filter(up -> ev.code.equals(up.code)).map(up -> 0).take(1)));

        Map<String, Integer> keys1 = new HashMap<>();
        keys1.put("KeyA", -1);
        keys1.put("KeyD", +1);
        Map<String, Integer> keys2 = new HashMap<>();
        keys2.put("KeyJ", -1);
        keys2.put("KeyL", +1);
        Observable<Integer> paddle1Directions = fromKey.apply(keys1);
        Observable<Integer> paddle2Directions = fromKey.apply(keys2);

        Observable<Consumer<State>> paddle1Position = ticker$
                .withLatestFrom(paddle1Directions, (ticker, direction) -> state -> {
                    if (direction == 0) return;
                    double position = state.paddle1 + (direction * ticker.delta * PADDLESPEED);
                    position = Math.max(Math.min(position, canvas.width - PADDLEWIDTH / 2), PADDLEWIDTH / 2);
                    state.paddle1 = position;
                });

        Observable<Consumer<State>> paddle2Position = ticker$
                .withLatestFrom(paddle2Directions, (ticker, direction) -> state -> {
                    if (direction == 0) return;
                    double position = state.paddle2 + (direction * ticker.delta * PADDLESPEED);
                    position = Math.max(Math.min(position, canvas.width - PADDLEWIDTH / 2), PADDLEWIDTH / 2);
                    state.paddle2 = position;
                });

        Observable<Consumer<State>> ballPosition = ticker$.map((Tick next) -> state -> {
            double nextX = state.ball.position.x + (state.ball.direction.x * next.delta * BALL_SPEED);
            double nextY = state.ball.position.y + (state.ball.direction.y * next.delta * BALL_SPEED);

            XY nextDir = state.ball.direction;
            if (BALLRADIUS / 2 > nextX || nextX > canvas.width - BALLRADIUS / 2) nextDir = new XY(nextDir.x * -1, nextDir.y);

            boolean over1 = nextY > canvas.height - PADDLEHEIGHT - BALLRADIUS;
            if (over1 && state.paddle1 - PADDLEWIDTH / 2 < nextX && nextX < state.paddle1 + PADDLEWIDTH / 2) {
                nextDir = new XY(nextDir.x, nextDir.y * -1);
                nextY = canvas.height - PADDLEHEIGHT - BALLRADIUS;
                playerToWin[0] = "Player 1";
            }

            boolean over2 = nextY < PADDLEHEIGHT + BALLRADIUS;
            if (over2 && state.paddle2 - PADDLEWIDTH / 2 < nextX && nextX < state.paddle2 + PADDLEWIDTH / 2) {
                nextDir = new XY(nextDir.x, nextDir.y * -1);
                nextY = PADDLEHEIGHT + BALLRADIUS;
                playerToWin[0] = "Player 2";
            }

            state.ball = new BallState(new XY(nextX, nextY), nextDir);
        });

        Observable<State> stateMachine = Observable.merge(paddle1Position, paddle2Position, ballPosition)
                .scanWith(initialState, (State state, Consumer<State> next) -> {
                    State out = state.copy();
                    next.accept(out);
                    return out;
                })
                .skip(1);

        Predicate<State> drawAll = game -> {

            context.clearRect(0, 0, canvas.width, canvas.height);
            drawPaddle1.accept(game.paddle1);
            drawPaddle2.accept(game.paddle2);
            drawBall.accept(game.ball.position);
            drawState.accept(game.toString());

            if (game.over())
                drawGameOver.accept("Game is over, " + playerToWin[0] + " wins!");
            return game.over();
        };

        Observable<State> game$ = Observable.defer(() -> {
            context.clearRect(0, 0, canvas.width, canvas.height);
            infoAuthor.run();
            infoControls.run();
            infoTitle.run();
            return stateMachine.takeUntil(drawAll);
        });

        game$.ignoreElements()
                .andThen(fromEvent(document, keydown).take(1))
                .repeat().subscribe();
    }

    static <T extends Event> Observable<T> fromEvent(EventTarget src, EventType<T, ?> type) {
        return Observable.create(s -> {
            EventListener listener = value -> s.onNext(Js.cast(value));
            src.addEventListener(type.getName(), listener, false);
            s.setCancellable(() -> src.removeEventListener(type.getName(), listener, false));
        });
    }

    private Resources.StylePong css() { return Resources.INSTANCE.style(); }

    public interface Resources extends ClientBundle {
        Resources INSTANCE = GWT.create(Resources.class);

        @Source("style.gss") StylePong style();

        interface StylePong extends CssResource {
            String board();
            String gamestatus();
        }
    }
}
