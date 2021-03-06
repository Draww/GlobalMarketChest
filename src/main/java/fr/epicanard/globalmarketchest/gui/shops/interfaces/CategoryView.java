package fr.epicanard.globalmarketchest.gui.shops.interfaces;

import java.util.function.Consumer;

import fr.epicanard.globalmarketchest.GlobalMarketChest;
import fr.epicanard.globalmarketchest.gui.CategoryHandler;
import fr.epicanard.globalmarketchest.gui.InventoryGUI;
import fr.epicanard.globalmarketchest.gui.TransactionKey;
import fr.epicanard.globalmarketchest.gui.actions.NextInterface;
import fr.epicanard.globalmarketchest.gui.shops.baseinterfaces.DefaultFooter;
import fr.epicanard.globalmarketchest.managers.GroupLevels;

public class CategoryView extends DefaultFooter {

  public CategoryView(InventoryGUI inv) {
    super(inv);
    this.actions.put(0, new NextInterface("SearchView"));
  }

  @Override
  public void load() {
    super.load();
    final Consumer<InventoryGUI> callable = new NextInterface("AuctionViewList");

    final CategoryHandler h = GlobalMarketChest.plugin.getCatHandler();
    final String[] categories = h.getCategories().toArray(new String[0]);

    for (String category : categories) {
      this.setCategory(category, callable);
    }
    if (GlobalMarketChest.plugin.getConfigLoader().getConfig().getBoolean("Options.UncategorizedItems"))
      this.setCategory("!", callable);
  }

  private void setCategory(final String category, final Consumer<InventoryGUI> callable) {
    final CategoryHandler h = GlobalMarketChest.plugin.getCatHandler();

    this.actions.put(h.getPosition(category), in -> {
      this.inv.getTransaction().put(TransactionKey.CATEGORY, category);
      this.inv.getTransaction().put(TransactionKey.AUCTIONITEM, GlobalMarketChest.plugin.getCatHandler().getDisplayItem(category));
      this.inv.getTransaction().put(TransactionKey.GROUPLEVEL, GroupLevels.LEVEL1);
      callable.accept(in);
    });

    this.inv.getInv().setItem(h.getPosition(category), h.getDisplayItem(category));
  }
}
