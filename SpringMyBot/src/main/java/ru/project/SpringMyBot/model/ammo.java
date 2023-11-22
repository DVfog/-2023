package ru.project.SpringMyBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
@Entity(name="ammo")
public class ammo {
    @Id
    private Integer id;
    private String name;
    private String caliber;
    private Long damage;
    private Long penetrationPower;

    public Integer getId() {
        return id;
    }

    public void Integer(String id) {
        this.id = Integer.valueOf(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaliber() {
        return caliber;
    }

    public void setCaliber(String shortname) {
        this.caliber = shortname;
    }

    public Long getDamage() {
        return damage;
    }

    public void setDamage(Long damage) {
        this.damage = damage;
    }

    public Long getPenetrationPower() {
        return penetrationPower;
    }

    public void setPenetrationPower(Long penetrationPower) {
        this.penetrationPower = penetrationPower;
    }
}
