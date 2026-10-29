package dev.draconicsable.explosion;

public interface BlastPlan {
    void draconicSable$prepare();
    boolean draconicSable$matches();
    void draconicSable$discard();
    int draconicSable$terrainTickets();
    BlastTrace draconicSable$takeTerrain();
    void draconicSable$reuseTerrain(BlastTrace previous);
}
