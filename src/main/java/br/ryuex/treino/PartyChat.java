package br.ryuex.treino;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
final class PartyChat {
    static BaseComponent[] invitation(UUID leader,String name) {
        TextComponent accept=new TextComponent("[ACEITAR]");
        accept.setColor(ChatColor.GREEN);accept.setBold(true);
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/party aceitar "+leader));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{new TextComponent("Entrar na party de "+name+" • expira em 60s")}));
        TextComponent decline=new TextComponent("   [RECUSAR]");decline.setColor(ChatColor.RED);
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/party recusar "+leader));
        return new BaseComponent[]{accept,decline};
    }
}
