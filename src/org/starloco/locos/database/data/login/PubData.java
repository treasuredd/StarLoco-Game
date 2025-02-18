package org.starloco.locos.database.data.login;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.NotImplementedException;
import org.starloco.locos.database.data.FunctionDAO;
import org.starloco.locos.game.scheduler.entity.WorldPub;
import org.starloco.locos.kernel.Config;

import java.sql.SQLException;

public class PubData extends FunctionDAO<Object> {

    public PubData(HikariDataSource dataSource)
	{
		super(dataSource, "world_pubs");
	}

    @Override
    public void loadFully() {
        try {
            getData("SELECT * FROM " + getTableName() + " WHERE `server` = " + Config.gameServerId, result -> {
                while (result.next()) {
                    WorldPub.pubs.add(result.getString("data"));
                }
            });
        } catch (SQLException e) {
            super.sendError(e);
        }
    }

    @Override
    public Object load(int id) {
        throw new NotImplementedException();
    }

    @Override
    public boolean insert(Object entity) {
        throw new NotImplementedException();
    }

    @Override
    public void delete(Object entity) {
        throw new NotImplementedException();
    }

    @Override
    public void update(Object entity) {
        throw new NotImplementedException();
    }

    @Override
    public Class<?> getReferencedClass() {
        return PubData.class;
    }
}
