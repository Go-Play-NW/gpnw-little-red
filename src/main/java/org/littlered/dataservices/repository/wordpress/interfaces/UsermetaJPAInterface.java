package org.littlered.dataservices.repository.wordpress.interfaces;

import org.littlered.dataservices.entity.wordpress.Usermeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsermetaJPAInterface extends JpaRepository<Usermeta, Long> {

	@Query(value = "select um from Usermeta um where um.userId = ?1 and um.metaKey = ?2")
	List<Usermeta> findUsermetaByUserIdAndMetaKey(Long userId, String metaKey);

	List<Usermeta> findUsermetasByMetaKeyAndMetaValue(String metaKey, String metaValue);

	List<Usermeta> findUsermetasByMetaKey(String metaKey);

}
