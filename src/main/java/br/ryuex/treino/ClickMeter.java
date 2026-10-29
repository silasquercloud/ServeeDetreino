package br.ryuex.treino;
import java.util.*;

/** Counts observed server actions in a bounded rolling one-second window. */
final class ClickMeter {
    private final Deque<Long> left=new ArrayDeque<>(),right=new ArrayDeque<>();
    private void trim(Deque<Long> queue,long now){while(!queue.isEmpty() && queue.peekFirst()<=now-1000)queue.removeFirst();}
    void record(boolean rightClick,long now){Deque<Long> queue=rightClick?right:left;trim(queue,now);if(queue.size()<256)queue.addLast(now);}
    int count(boolean rightClick,long now){Deque<Long> queue=rightClick?right:left;trim(queue,now);return queue.size();}
}
