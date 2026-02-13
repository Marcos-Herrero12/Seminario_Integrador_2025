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

	@Select("SELECT * FROM Empresa WHERE cuit = #{cuit}")
	Empresa findByCuit(@Param("cuit") String cuit);

	@Select("SELECT * FROM Empresa WHERE nombre = #{nombre}")
	List<Empresa> findByNombre(@Param("nombre") String nombre);

	@Select("SELECT * FROM Empresa WHERE id_usuario = #{idUsuario}")
	Empresa findByIdUsuario(@Param("idUsuario") Integer idUsuario);

	@Insert("INSERT INTO Empresa(nombre, ciudad, calle, nro_calle, piso, departamento, barrio, email, cuit, razon_social, id_usuario, activo, fecha_creacion) VALUES(#{nombre}, #{ciudad}, #{calle}, #{nroCalle}, #{piso}, #{departamento}, #{barrio}, #{email}, #{cuit}, #{razonSocial}, #{idUsuario}, #{activo}, #{fechaCreacion})")
	@Options(useGeneratedKeys = true, keyProperty = "idEmpresa")
	void insert(Empresa empresa);

	@Update("UPDATE Empresa SET nombre=#{nombre}, ciudad=#{ciudad}, calle=#{calle}, nro_calle=#{nroCalle}, piso=#{piso}, departamento=#{departamento}, barrio=#{barrio}, email=#{email}, cuit=#{cuit}, razon_social=#{razonSocial}, activo=#{activo} WHERE id_empresa=#{idEmpresa}")
	void update(Empresa empresa);

	@Delete("DELETE FROM Empresa WHERE id_empresa = #{id}")
	void delete(@Param("id") Integer id);

	@Delete("DELETE FROM Empresa WHERE cuit = #{cuit}")
	void deleteByCuit(@Param("cuit") String cuit);
}
