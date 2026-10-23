package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

public final class EventConditionHeadshot extends AbstractEventCondition {

    public EventConditionHeadshot() {
        super("headshot");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null || !(condition.getContext().target() instanceof LivingEntity target)) {
            return false;
        }
        var event = condition.getContext().event();
        Projectile projectile = event instanceof EntityDamageByEntityEvent damage && damage.getDamager() instanceof Projectile shot ? shot : event instanceof ProjectileHitEvent hit ? hit.getEntity() : null;
        if (projectile == null) {
            return false;
        }
        double tolerance = condition.getDouble("tolerance", 0.45D, condition.getContext());
        return Math.abs(projectile.getLocation().getY() - target.getEyeLocation().getY()) <= tolerance;
    }
}
