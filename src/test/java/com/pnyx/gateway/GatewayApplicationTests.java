package com.pnyx.gateway;

import com.pnyx.gateway.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class GatewayApplicationTests {

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private RefreshTokenRepository refreshTokenRepository;

	@MockitoBean
	private ServiceEntryRepository serviceEntryRepository;

	@MockitoBean
	private ImageRepository imageRepository;

	@MockitoBean
	private VehicleRepository vehicleRepository;

	@Test
	void contextLoads() {
	}

}
