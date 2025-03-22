package org.littlered.dataservices.service;

import org.littlered.dataservices.entity.wordpress.Options;
import org.littlered.dataservices.repository.eventManager.interfaces.OptionsRepositoryInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class OptionsService {

	@Autowired
	public OptionsRepositoryInterface optionsRepository;

	public Options getOptionsByOptionName(String optionName) {
		return optionsRepository.findByName(optionName);
	}

	public void saveOptions(Options options) {
		optionsRepository.save(options);
	}

}
