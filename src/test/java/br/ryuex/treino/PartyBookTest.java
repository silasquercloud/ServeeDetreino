package br.ryuex.treino;
import java.util.*;
import net.md_5.bungee.api.chat.*;
public final class PartyBookTest {
    static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    static void rejected(Runnable action,String message) {
        try {action.run();}catch(IllegalArgumentException expected) {return;}
        throw new AssertionError(message);
    }
    public static void main(String[] args) {
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),d=UUID.randomUUID();
        PartyBook book=new PartyBook();
        rejected(()->book.accept(b,a,0),"Cannot accept without invitation");
        book.invite(a,b,0);
        rejected(()->book.invite(a,b,100),"Invitation spam must be limited");
        rejected(()->book.accept(c,a,0),"Only the invited player can accept");
        book.accept(b,a,1);
        check(book.members(a).size()==2 && book.get(a)==book.get(b),"Leader and invitee share party");
        rejected(()->book.invite(b,c,0),"Members cannot issue leader invitations");
        book.invite(a,c,0);book.promote(a,b);
        rejected(()->book.accept(c,a,1),"Promotion invalidates old clickable invitation");
        book.invite(b,c,100);book.accept(c,b,101);book.leave(b);
        check(book.isLeader(a) && book.get(c)==book.get(a),"Leader disconnect promotes remaining member");
        book.kick(a,c);check(book.get(c)==null,"Kicked member removed");
        book.invite(a,d,1000);book.disband(a);
        rejected(()->book.accept(d,a,1001),"Disband invalidates invitations");
        book.invite(a,d,2000);
        rejected(()->book.accept(d,a,62000),"Invitation expires at 60 seconds");
        book.invite(a,d,63000);book.decline(d,a);
        rejected(()->book.accept(d,a,63001),"Declined invitation cannot be accepted");
        PartyBook full=new PartyBook();
        for(int i=0;i<11;i++) {UUID member=UUID.randomUUID();full.invite(a,member,0);full.accept(member,a,1);}
        rejected(()->full.invite(a,b,2),"Capacity is capped at 12");
        check(PartyBook.fits(4,2,4,6),"Members already in room must not consume two slots");
        check(!PartyBook.fits(4,0,4,6),"Do not split party when room lacks space");
        BaseComponent[] button=PartyChat.invitation(a,"anything /op x");
        check(button[0].getClickEvent().getAction()==ClickEvent.Action.RUN_COMMAND,"Accept is clickable");
        check(button[0].getClickEvent().getValue().equals("/party aceitar "+a),"Click targets UUID, not untrusted display name");
        check(button[1].getClickEvent().getValue().equals("/party recusar "+a),"Decline button works");
        System.out.println("PartyBookTest: OK — click actions, invitation ownership/expiry, roles, capacity and cleanup");
    }
}
