package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration tests for the Spring Boot application.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InternshipApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ItemService itemService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
		// Smoke test to ensure the Spring context loads
		assertThat(itemService).isNotNull();
	}

	@Test
	void createValidItem_ShouldReturnCreated() throws Exception {
		Item item = new Item(null, "Test Item", "This is a test", "PENDING", "test@example.com");

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(item)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Test Item"));
	}

	@Test
	void getItemById_ShouldReturnItem_WhenExists() throws Exception {
		Item saved = itemService.save(new Item(null, "Lookup", "Find me", "PENDING", "lookup@example.com"));

		mockMvc.perform(get("/api/items/" + saved.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Lookup"));
	}

	@Test
	void getItemById_ShouldReturnNotFound_WhenNotExists() throws Exception {
		mockMvc.perform(get("/api/items/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateItem_ShouldModifyExisting() throws Exception {
		Item item = itemService.save(new Item(null, "Before Update", "Old", "PENDING", "update@example.com"));
		item.setName("After Update");

		mockMvc.perform(put("/api/items/" + item.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(item)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("After Update"));
	}

	@Test
	void deleteItem_ShouldRemoveSuccessfully() throws Exception {
		Item item = itemService.save(new Item(null, "To Be Deleted", "Bye", "PENDING", "delete@example.com"));

		mockMvc.perform(delete("/api/items/" + item.getId()))
				.andExpect(status().isNoContent());

		Assertions.assertFalse(itemService.findById(item.getId()).isPresent());
	}

	@Test
	void processItemsAsync_ShouldUpdateStatus() throws Exception {
		Item i1 = itemService.save(new Item(null, "Process Me", "A", "PENDING", "a@example.com"));
		Item i2 = itemService.save(new Item(null, "Process Me Too", "B", "PENDING", "b@example.com"));

		mockMvc.perform(get("/api/items/process"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[*].status").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("PROCESSED"))));

		List<Item> updated = itemService.findAll();
		updated.forEach(item -> assertThat(item.getStatus()).isEqualTo("PROCESSED"));
	}
}

