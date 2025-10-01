package com.seminario.pasantias.persistence;

import com.seminario.pasantias.entity.Empresa;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface EmpresaMapper {
	@Select("SELECT * FROM Empresa")
	List<Empresa> findAll();

	@Select("SELECT * FROM Empresa WHERE id_empresa = #{id}")
	Empresa findById(@Param("id") Integer id);

	@Insert("INSERT INTO Empresa(nombre, ciudad, direccion, email_contacto, cuit, razonSocial) VALUES(#{nombre}, #{ciudad}, #{direccion}, #{emailContacto}, #{cuit}, #{razonSocial})")
	@Options(useGeneratedKeys = true, keyProperty = "idEmpresa")
	void insert(Empresa empresa);

	@Update("UPDATE Empresa SET nombre=#{nombre}, ciudad=#{ciudad}, direccion=#{direccion}, email_contacto=#{emailContacto}, cuit=#{cuit}, razonSocial=#{razonSocial} WHERE id_empresa=#{idEmpresa}")
	void update(Empresa empresa);

	@Delete("DELETE FROM Empresa WHERE id_empresa = #{id}")
	void delete(@Param("id") Integer id);
}
