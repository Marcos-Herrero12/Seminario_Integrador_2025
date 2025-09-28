package com.seminario.pasantias.persistence;

import com.seminario.pasantias.entity.Carrera;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CarreraMapper {
    @Select("SELECT * FROM Carrera")
    List<Carrera> findAll();

    @Select("SELECT * FROM Carrera WHERE id_carrera = #{id}")
    Carrera findById(@Param("id") Integer id);

    @Select("SELECT * FROM Carrera WHERE nombre = #{nombre}")
    Optional<Carrera> findByNombre(@Param("nombre") String nombre);

    @Insert("INSERT INTO Carrera(nombre) VALUES(#{nombre})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Carrera carrera);

    @Delete("DELETE FROM Carrera WHERE id_carrera = #{id}")
    void delete(@Param("id") Integer id);

    @Update("UPDATE Carrera SET nombre = #{nombre} WHERE id = #{id}")
    void update(Carrera carrera);

    @Delete("DELETE FROM Carrera WHERE nombre = #{nombre}")
    void deleteByNombre(@Param("nombre") String nombre);
}