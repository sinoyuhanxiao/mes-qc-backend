package com.fps.svmes.models.sql.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

@Entity
@Table(name = "user", schema = "quality_management")
@Data
public class User {
    @Id
    @JsonProperty("id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @JsonProperty("name")
    @Column(name = "name")
    private String name;

    @JsonProperty("role_id")
    @Column(name = "role_id")
    private Short roleId;

    @JsonProperty("wecom_id")
    @Column(name = "wecom_id")
    private String wecomId;

    @JsonProperty("username")
    @Column(name = "username")
    private String username;

    @JsonProperty("password")
    @Column(name = "password")
    private String password;

}
