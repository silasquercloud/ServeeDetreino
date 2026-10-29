package br.ryuex.treino;

import java.util.*;

/** Main-thread party state; no Bukkit dependency. Invitations belong to a specific party. */
final class PartyBook {
    static final int LIMIT=12;
    static final long EXPIRY=60000L;
    static final class Party {
        UUID leader;
        final LinkedHashSet<UUID> members=new LinkedHashSet<>();
        Party(UUID leader) { this.leader=leader; members.add(leader); }
    }
    private static final class Invite {
        final Party party; final long created;
        Invite(Party party,long created) { this.party=party;this.created=created; }
    }
    private final Map<UUID,Party> players=new HashMap<>();
    private final Map<UUID,Map<UUID,Invite>> invitations=new HashMap<>();
    Party get(UUID player) { return players.get(player); }
    boolean isLeader(UUID player) { Party p=get(player);return p!=null && p.leader.equals(player); }
    List<UUID> members(UUID player) {
        Party p=get(player);if(p==null)return Collections.singletonList(player);
        List<UUID> out=new ArrayList<>();out.add(p.leader);
        for(UUID member:p.members) if(!member.equals(p.leader))out.add(member);
        return Collections.unmodifiableList(out);
    }
    void invite(UUID leader,UUID target,long now) {
        prune(now);
        if(leader.equals(target))throw new IllegalArgumentException("Você não pode convidar a si mesmo.");
        Party p=get(leader);
        if(p!=null && !p.leader.equals(leader))throw new IllegalArgumentException("Só o líder pode convidar.");
        if(get(target)!=null)throw new IllegalArgumentException("Esse jogador já está em uma party.");
        if(p==null) {p=new Party(leader);players.put(leader,p);}
        if(p.members.size()>=LIMIT)throw new IllegalArgumentException("A party já tem 12 jogadores.");
        Map<UUID,Invite> pending=invitations.computeIfAbsent(target,k->new HashMap<>());
        Invite previous=pending.get(leader);
        if(previous!=null && now-previous.created<10000L)throw new IllegalArgumentException("Aguarde 10 segundos para reenviar o convite.");
        pending.put(leader,new Invite(p,now));
    }
    void accept(UUID target,UUID leader,long now) {
        prune(now);
        if(get(target)!=null)throw new IllegalArgumentException("Saia da sua party atual primeiro.");
        Map<UUID,Invite> pending=invitations.get(target);Invite invite=pending==null?null:pending.get(leader);
        if(invite==null || get(leader)!=invite.party || !invite.party.leader.equals(leader))throw new IllegalArgumentException("Convite expirado ou inválido. Peça um novo convite.");
        if(invite.party.members.size()>=LIMIT)throw new IllegalArgumentException("Essa party ficou cheia.");
        invite.party.members.add(target);players.put(target,invite.party);invitations.remove(target);
    }
    void decline(UUID target,UUID leader) {
        Map<UUID,Invite> pending=invitations.get(target);
        if(pending==null || pending.remove(leader)==null)throw new IllegalArgumentException("Convite não encontrado.");
        if(pending.isEmpty())invitations.remove(target);
    }
    void requireLeader(UUID leader) {
        if(!isLeader(leader))throw new IllegalArgumentException("Só o líder da party pode fazer isso.");
    }
    void promote(UUID leader,UUID target) {
        requireLeader(leader);Party p=get(leader);
        if(!p.members.contains(target))throw new IllegalArgumentException("Esse jogador não está na sua party.");
        if(leader.equals(target))throw new IllegalArgumentException("Você já é o líder.");
        clearInvites(p);p.leader=target;
    }
    void kick(UUID leader,UUID target) {
        requireLeader(leader);
        if(leader.equals(target))throw new IllegalArgumentException("Use /party sair para sair.");
        if(get(target)!=get(leader))throw new IllegalArgumentException("Esse jogador não está na sua party.");
        leave(target);
    }
    void leave(UUID player) {
        invitations.remove(player);Party p=players.remove(player);
        if(p==null)return;
        p.members.remove(player);clearInvitesFrom(player);
        if(p.members.isEmpty()) {clearInvites(p);return;}
        if(p.leader.equals(player)) {clearInvites(p);p.leader=p.members.iterator().next();}
    }
    void disband(UUID leader) {
        requireLeader(leader);Party p=get(leader);
        for(UUID member:p.members)players.remove(member);
        clearInvites(p);
    }
    private void clearInvitesFrom(UUID leader) {
        for(Map<UUID,Invite> pending:invitations.values())pending.remove(leader);
        invitations.values().removeIf(Map::isEmpty);
    }
    private void clearInvites(Party party) {
        for(Map<UUID,Invite> pending:invitations.values())pending.values().removeIf(i->i.party==party);
        invitations.values().removeIf(Map::isEmpty);
    }
    void prune(long now) {
        for(Map<UUID,Invite> pending:invitations.values())pending.values().removeIf(i->now-i.created>=EXPIRY);
        invitations.values().removeIf(Map::isEmpty);
    }
    static boolean fits(int occupied,int alreadyPresent,int groupSize,int capacity) { return occupied+groupSize-alreadyPresent<=capacity; }
}
