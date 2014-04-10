package org.javers.core.metamodel.object;

import org.javers.common.exception.exceptions.JaversException;
import org.javers.common.exception.exceptions.JaversExceptionCode;
import org.javers.common.validation.Validate;
import org.javers.core.metamodel.property.Entity;
import org.javers.core.metamodel.property.ManagedClass;
import org.javers.core.metamodel.property.ValueObject;
import org.javers.core.metamodel.type.JaversType;
import org.javers.core.metamodel.type.ManagedType;
import org.javers.core.metamodel.type.TypeMapper;

/**
 * @author bartosz walacik
 */
public class GlobalIdFactory {

    private final TypeMapper typeMapper;

    public GlobalIdFactory(TypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }

    public GlobalCdoId createId(Object targetCdo) {
        return createId(targetCdo, null);
    }

    /**
     * @param owner for bounded ValueObjects, optional
     */
    public GlobalCdoId createId(Object targetCdo, OwnerContext owner) {
        Validate.argumentsAreNotNull(targetCdo);

        ManagedClass targetManagedClass = getManagedClassOf(targetCdo);

        if (targetManagedClass instanceof Entity) {
            return InstanceId.createFromInstance(targetCdo, (Entity) targetManagedClass);
        }

        if (targetManagedClass instanceof ValueObject && hasNoOwner(owner)) {
            return new UnboundedValueObjectId((ValueObject)targetManagedClass);
        }

        if (targetManagedClass instanceof ValueObject && hasOwner(owner)) {
            return new ValueObjectId((ValueObject) targetManagedClass, owner);
        }

        throw new JaversException(JaversExceptionCode.NOT_IMPLEMENTED);
    }

    public ValueObjectId createFromPath(InstanceId owner, Class valueObjectClass, String path){
        ManagedClass targetManagedClass = getManagedClass(valueObjectClass);
        return new ValueObjectId((ValueObject) targetManagedClass, owner, path);
    }

    public InstanceId createFromId(Object localId, Class entityClass){
        ManagedClass targetManagedClass = getManagedClass(entityClass);
        return InstanceId.createFromId(localId, (Entity)targetManagedClass);
    }

    private ManagedClass getManagedClassOf(Object cdo) {
        Validate.argumentIsNotNull(cdo);
        return  getManagedClass(cdo.getClass());
    }

    /**
     * if given javaClass is mapped to {@link ManagedType}
     * returns {@link ManagedType#getManagedClass()}
     * @throws java.lang.IllegalArgumentException if given javaClass is NOT mapped to {@link ManagedType}
     */
    public ManagedClass getManagedClass(Class javaClass) {
        JaversType jType = typeMapper.getJaversType(javaClass);
        if (jType instanceof ManagedType) {
            return ((ManagedType)jType).getManagedClass();
        }
        throw new IllegalArgumentException("getManagedClass("+javaClass.getSimpleName()+") " +
                "given javaClass is mapped to "+jType.getClass().getSimpleName()+", ManagedType expected");
    }

    private boolean hasOwner(OwnerContext context) {
        return (context != null && context.getGlobalCdoId() != null);
    }

    private boolean hasNoOwner(OwnerContext context) {
        return (context == null || context.getGlobalCdoId() == null);
    }
}
