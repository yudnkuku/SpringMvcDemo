package spring.dao;

import org.apache.ibatis.annotations.Param;
import spring.entity.LevelEnum;

public interface IEnumDao {

    void addLevel(@Param("level_value") LevelEnum level);

}
