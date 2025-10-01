package com.gantoniadis.cargopulse.user.service;

import com.gantoniadis.cargopulse.user.dto.AddUserRequestDTO;
import com.gantoniadis.cargopulse.user.dto.RoleDTO;
import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;
import com.gantoniadis.cargopulse.user.entity.UserAccount;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface defining the contract for User Account business logic operations.
 */
public interface UserService {

    /**
     * Retrieves a list of all user accounts as DTOs.
     * @return A list of UserAccountDTOs.
     */
    List<UserAccountDTO> getAllUsers();

    /**
     * Finds a UserAccount entity by its username.
     * @param username The username to search for.
     * @return An Optional containing the UserAccount entity, or empty if not found.
     */
    Optional<UserAccount> findUserByUsername(String username);

    /**
     * Creates a new user based on the request data.
     * @param request The DTO containing the user creation details.
     * @param regularRegister If true, removes the 'Admin' role from the request.
     * @return The DTO representing the newly created user's response.
     */
    UserAccountDTO createNewUser(AddUserRequestDTO request, boolean regularRegister);

    /**
     * Retrieves the total count of users in the repository.
     * @return The count of user accounts.
     */
    long getUserRepositoryCount();

    /**
     * Saves a collection of roles to the repository.
     * @param roleDTOList A Set of RoleDTOs to be saved.
     */
    void saveAllRoles(Set<RoleDTO> roleDTOList);
}
