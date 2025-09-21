package com.seminario.pasantias.service;

import com.seminario.pasantias.entity.Empresa;
import com.seminario.pasantias.persistence.EmpresaMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Service
public class EmpresaService {
	private final EmpresaMapper empresaMapper;

	@Autowired
	public EmpresaService(EmpresaMapper empresaMapper) {
		this.empresaMapper = empresaMapper;
	}

	public List<Empresa> getAllEmpresas() {
		return empresaMapper.findAll();
	}

	public Empresa getEmpresaById(Integer id) {
		return empresaMapper.findById(id);
	}

	public void createEmpresa(Empresa empresa) {
		empresaMapper.insert(empresa);
	}

	public void updateEmpresa(Empresa empresa) {
		empresaMapper.update(empresa);
	}

	public void deleteEmpresa(Integer id) {
		empresaMapper.delete(id);
	}
}
