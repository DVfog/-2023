package ru.project.SpringMyBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
@Entity(name="ammo")
public class ammo {
    @Id
    private Integer id;
    private String name;
    private String caliber;
    private String damage;
    private String penetrationPower;

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

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDamage() {
        return damage;
    }

    public void setDamage(String damage) {
        this.damage = damage;
    }

    public String getPenetrationPower() {
        return penetrationPower;
    }

    public void setPenetrationPower(String penetrationPower) {
        this.penetrationPower = penetrationPower;
    }
}
