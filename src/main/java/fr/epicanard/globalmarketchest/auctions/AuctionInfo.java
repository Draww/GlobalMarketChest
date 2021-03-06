package fr.epicanard.globalmarketchest.auctions;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.epicanard.globalmarketchest.GlobalMarketChest;
import fr.epicanard.globalmarketchest.utils.DatabaseUtils;
import fr.epicanard.globalmarketchest.utils.ItemStackUtils;
import fr.epicanard.globalmarketchest.utils.LangUtils;
import fr.epicanard.globalmarketchest.utils.PlayerUtils;
import lombok.Getter;
import lombok.Setter;

public class AuctionInfo {
  @Getter
  private Integer id;
  @Getter
  private String itemStack;
  @Getter @Setter
  private String itemMeta;
  @Getter @Setter
  private Integer amount;
  @Getter @Setter
  private Double price;
  @Getter
  private StateAuction state;
  @Getter
  private Boolean ended;
  @Getter
  private AuctionType type;
  @Getter
  private String playerStarter;
  @Getter
  private String playerEnder;
  @Getter
  private Timestamp start;
  @Getter
  private Timestamp end;
  @Getter
  private String group;

  public AuctionInfo(ResultSet res) {
    if (res == null)
      throw new NullPointerException("Fail to get auction from database");
    this.id = DatabaseUtils.getField("id", res::getInt);
    this.itemStack = DatabaseUtils.getField("itemStack", res::getString);
    this.itemMeta = DatabaseUtils.getField("itemMeta", res::getString);
    this.amount = DatabaseUtils.getField("amount", res::getInt);
    this.price = DatabaseUtils.getField("price", res::getDouble);
    this.ended = DatabaseUtils.getField("ended", res::getBoolean);
    this.type = AuctionType.getAuctionType(DatabaseUtils.getField("type", res::getInt));
    this.playerStarter = DatabaseUtils.getField("playerStarter", res::getString);
    this.playerEnder = DatabaseUtils.getField("playerEnder", res::getString);
    this.start = DatabaseUtils.getField("start", res::getTimestamp);
    this.end = DatabaseUtils.getField("end", res::getTimestamp);
    this.group = DatabaseUtils.getField("group", res::getString);
    this.state = StateAuction.getStateAuction(this);

    if (this.itemMeta == null && this.itemStack != null) {
      this.itemMeta = DatabaseUtils.serialize(ItemStackUtils.getItemStack(this.itemStack));
    }
  }

  public AuctionInfo(AuctionType type, Player owner, String group) {
    this.state = StateAuction.INPROGRESS;
    this.type = AuctionType.getAuctionType(type.getType());
    this.price = GlobalMarketChest.plugin.getConfigLoader().getConfig().getDouble("Options.DefaultPrice", 0.0);
    this.playerStarter = owner.getUniqueId().toString();
    this.ended = false;
    this.group = group;
  }

  public Double getTotalPrice() {
    return BigDecimal.valueOf(this.price).multiply(BigDecimal.valueOf(this.amount)).doubleValue();
  }

  public void setItemStack(ItemStack item) {
    ItemStack it = item.clone();
    it.setAmount(1);
    this.itemStack = ItemStackUtils.getMinecraftKey(it);
    this.itemMeta = DatabaseUtils.serialize(it);
  }

  private String checkPrice(double price) {
    if (price == 0.0)
      return "&2" + LangUtils.get("Divers.Free");
    return Double.toString(price);
  }

  public ItemStack[] getRealItemStack() {
    List<ItemStack> items = new ArrayList<>();

    ItemStack item = DatabaseUtils.deserialize(this.itemMeta);
    int amount = this.amount;
    int stackSize = item.getMaxStackSize();
    while (amount > 0) {
      ItemStack it = item.clone();
      it.setAmount(ItemStackUtils.getMaxStack(it, amount));
      items.add(it);
      amount -= stackSize;
    }
    return items.toArray(new ItemStack[0]);
  }

  private void addLore(List<String> lore, String key, String color, String value) {
    lore.add(String.format("&7%s : %s%s", LangUtils.get("Divers." + key), color, value));
  }

  /**
   * Build and return lore for current auction
   *
   * @param status
   * @return the lore
   */
  public List<String> getLore(AuctionLoreConfig config) {
    List<String> lore = new ArrayList<>();

    double totalPrice = BigDecimal.valueOf(this.price).multiply(BigDecimal.valueOf(this.amount)).doubleValue();
    if (config.getFrame())
      lore.add("&6--------------");
    if (config.getState())
      this.addLore(lore, "State", "&2", this.state.getLang());
    if (config.getQuantity())
      this.addLore(lore, "Quantity", "&6", this.amount.toString());
    if (config.getUnitPrice())
      this.addLore(lore, "UnitPrice", "&c", this.checkPrice(this.price));
    if (config.getTotalPrice())
      this.addLore(lore, "TotalPrice", "&c", this.checkPrice(totalPrice));
    if (config.getStarter())
      this.addLore(lore, "Seller", "&9", PlayerUtils.getPlayerName(this.playerStarter));
    if (config.getEnder())
      this.addLore(lore, "Buyer", "&9", PlayerUtils.getPlayerName(this.playerEnder));
    if (config.getStarted())
      this.addLore(lore, "Started", "&6",
        DatabaseUtils.getExpirationString(this.start, DatabaseUtils.getTimestamp(), false));
    if (config.getEnded())
      this.addLore(lore, "Ended", "&6",
        DatabaseUtils.getExpirationString(this.end, DatabaseUtils.getTimestamp(), false));
    if (config.getExpire()) {
      String path = (this.end.getTime() < DatabaseUtils.getTimestamp().getTime()) ? "Expired" : "ExpireIn";
      this.addLore(lore, path, "&6",
        DatabaseUtils.getExpirationString(this.end, DatabaseUtils.getTimestamp(), false));
    }
    if (config.getFrame())
      lore.add("&6--------------");
    lore.add(GlobalMarketChest.plugin.getCatHandler().getDisplayCategory(this.itemStack));
    return lore;
  }
}
