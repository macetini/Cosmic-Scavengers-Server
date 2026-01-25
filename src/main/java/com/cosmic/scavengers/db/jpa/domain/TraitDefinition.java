package com.cosmic.scavengers.db.jpa.domain;

import java.util.Map;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "trait_definitions")
public class TraitDefinition {

	@Id
	private String id;

	@Column(nullable = false)
	private String category;

	// @Convert(converter = JsonToMapConverter.class)
	// @Column(columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "data", columnDefinition = "jsonb")
	private Map<String, Object> data;

	// Default Constructor (Required by JPA)
	public TraitDefinition() {
	}

	// Full Constructor
	public TraitDefinition(String id, String category, Map<String, Object> data) {
		this.id = id;
		this.category = category;
		this.data = data;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TraitDefinition that = (TraitDefinition) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "TraitDefinition{" + "id='" + id + '\'' + ", category='" + category + '\'' + ", data=" + data + '}';
	}
}