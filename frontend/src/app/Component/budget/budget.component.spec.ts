import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BudgetComponent } from './budget.component';
import { BudgetService } from '../service/budget.service';
import { CostService } from '../../services/cost.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { Budget } from '../../models/budget.model';
import gsap from 'gsap';

describe('BudgetComponent', () => {
  let component: BudgetComponent;
  let fixture: ComponentFixture<BudgetComponent>;
  let budgetServiceSpy: jasmine.SpyObj<BudgetService>;
  let costServiceSpy: jasmine.SpyObj<CostService>;

  // ── Hilfsfunktion: Budget erstellen ──────────────────────────
  function makeBudget(overrides: Partial<Budget> = {}): Budget {
    return {
      id: 'b1',
      name: 'Test Budget',
      amount: 1000,
      costs: [],
      travelEntry: {
        id: 'te1',
        startDate: '2024-01-01',
        endDate: '2024-01-10',
        color: '#FF0000'
      },
      ...overrides
    };
  }

  beforeEach(async () => {
    budgetServiceSpy = jasmine.createSpyObj('BudgetService', ['getAll', 'create', 'update', 'delete']);
    costServiceSpy   = jasmine.createSpyObj('CostService',   ['addCostToBudget', 'deleteCost']);

    budgetServiceSpy.getAll.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [BudgetComponent],
      providers: [
        { provide: BudgetService, useValue: budgetServiceSpy },
        { provide: CostService,   useValue: costServiceSpy   },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(BudgetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── calculateTotal ───────────────────────────────────────────

  it('calculateTotal() should return sum of costs in costsMap', () => {
    const budget = makeBudget({ id: 'b1' });
    component.costsMap['b1'] = [
      { id: 'c1', name: 'Hotel',  amount: 300, budget },
      { id: 'c2', name: 'Flight', amount: 200, budget }
    ];

    expect(component.calculateTotal(budget)).toBe(500);
  });

  it('calculateTotal() should return 0 when no costs exist', () => {
    const budget = makeBudget({ id: 'b1' });
    component.costsMap['b1'] = [];

    expect(component.calculateTotal(budget)).toBe(0);
  });

  // ── KPI Getters ──────────────────────────────────────────────

  it('totalBudget should return sum of all budget amounts', () => {
    component.budgets = [
      makeBudget({ id: 'b1', amount: 500  }),
      makeBudget({ id: 'b2', amount: 1500 })
    ];
    expect(component.totalBudget).toBe(2000);
  });

  it('totalSpent should return sum of all calculateTotal results', () => {
    const b1 = makeBudget({ id: 'b1', amount: 1000 });
    const b2 = makeBudget({ id: 'b2', amount: 500  });
    component.budgets = [b1, b2];
    component.costsMap['b1'] = [{ id: 'c1', name: 'Hotel', amount: 400, budget: b1 }];
    component.costsMap['b2'] = [{ id: 'c2', name: 'Food',  amount: 100, budget: b2 }];

    expect(component.totalSpent).toBe(500);
  });

  it('overBudgetCount should count budgets where spent > amount', () => {
    const b1 = makeBudget({ id: 'b1', amount: 100 });
    const b2 = makeBudget({ id: 'b2', amount: 500 });
    component.budgets = [b1, b2];
    component.costsMap['b1'] = [{ id: 'c1', name: 'Over', amount: 200, budget: b1 }];
    component.costsMap['b2'] = [{ id: 'c2', name: 'Ok',   amount: 100, budget: b2 }];

    expect(component.overBudgetCount).toBe(1);
  });

  it('overBudgetCount should be 0 when no budget is exceeded', () => {
    const b1 = makeBudget({ id: 'b1', amount: 1000 });
    component.budgets = [b1];
    component.costsMap['b1'] = [{ id: 'c1', name: 'Hotel', amount: 200, budget: b1 }];

    expect(component.overBudgetCount).toBe(0);
  });

  it('savedBudgets should return only past budgets where spent <= amount', () => {
    const b1 = makeBudget({ id: 'b1', amount: 500, travelEntry: { id: 'te1', startDate: '2020-01-01', endDate: '2020-01-10' } });
    const b2 = makeBudget({ id: 'b2', amount: 100, travelEntry: { id: 'te2', startDate: '2020-02-01', endDate: '2020-02-10' } });
    component.budgets = [b1, b2];
    component.costsMap['b1'] = [{ id: 'c1', name: 'Hotel',     amount: 400, budget: b1 }];
    component.costsMap['b2'] = [{ id: 'c2', name: 'Overspend', amount: 200, budget: b2 }];

    expect(component.savedBudgets.length).toBe(1);
    expect(component.savedBudgets[0].id).toBe('b1');
  });

  // ── openDetailView / closeDetailView ─────────────────────────

  it('openDetailView() should set activeDetailView to saved', () => {
    component.openDetailView('saved');
    expect(component.activeDetailView).toBe('saved');
  });

  it('openDetailView() should set activeDetailView to overbudget', () => {
    component.openDetailView('overbudget');
    expect(component.activeDetailView).toBe('overbudget');
  });

  it('closeDetailView() should set activeDetailView to null when no DOM element', () => {
    component.activeDetailView = 'saved';
    component.closeDetailView();
    expect(component.activeDetailView).toBeNull();
  });

  // ── toggleTripGroup / toggleMonthGroup ────────────────────────

  it('toggleTripGroup() should toggle isOpen on a group', () => {
    const group = { label: 'Active', budgets: [], isOpen: true };
    component.toggleTripGroup(group);
    expect(group.isOpen).toBeFalse();

    component.toggleTripGroup(group);
    expect(group.isOpen).toBeTrue();
  });

  it('toggleMonthGroup() should toggle isOpen on a month group', () => {
    const group = { label: 'June 2025', year: 2025, month: 5, budgets: [], isOpen: false };
    component.toggleMonthGroup(group);
    expect(group.isOpen).toBeTrue();
  });

  // ── buildTripGroups ───────────────────────────────────────────

  it('buildTripGroups() should categorize budgets into Active, Upcoming and Past', () => {
    const today = new Date();

    const yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);

    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);

    const nextWeek = new Date(today);
    nextWeek.setDate(today.getDate() + 7);

    const lastMonth = new Date(today);
    lastMonth.setMonth(today.getMonth() - 1);

    const pastBudget = makeBudget({
      id: 'b-past',
      travelEntry: {
        id: 'te1',
        startDate: lastMonth.toISOString().split('T')[0],
        endDate:   yesterday.toISOString().split('T')[0]
      }
    });

    const activeBudget = makeBudget({
      id: 'b-active',
      travelEntry: {
        id: 'te2',
        startDate: yesterday.toISOString().split('T')[0],
        endDate:   tomorrow.toISOString().split('T')[0]
      }
    });

    const upcomingBudget = makeBudget({
      id: 'b-upcoming',
      travelEntry: {
        id: 'te3',
        startDate: tomorrow.toISOString().split('T')[0],
        endDate:   nextWeek.toISOString().split('T')[0]
      }
    });

    budgetServiceSpy.getAll.and.returnValue(of([pastBudget, activeBudget, upcomingBudget]));
    component.loadBudgets();

    const activeGroup   = component.tripGroups.find(g => g.label === 'Active');
    const upcomingGroup = component.tripGroups.find(g => g.label === 'Upcoming');
    const pastGroup     = component.tripGroups.find(g => g.label === 'Past');

    expect(activeGroup?.budgets.length).toBe(1);
    expect(upcomingGroup?.budgets.length).toBe(1);
    expect(pastGroup?.budgets.length).toBe(1);
  });

  // ── buildMonthGroups ──────────────────────────────────────────

  it('buildMonthGroups() should group budgets by start month', () => {
    const b1 = makeBudget({ id: 'b1', travelEntry: { id: 'te1', startDate: '2025-06-01', endDate: '2025-06-10' } });
    const b2 = makeBudget({ id: 'b2', travelEntry: { id: 'te2', startDate: '2025-06-15', endDate: '2025-06-20' } });
    const b3 = makeBudget({ id: 'b3', travelEntry: { id: 'te3', startDate: '2025-07-01', endDate: '2025-07-10' } });

    budgetServiceSpy.getAll.and.returnValue(of([b1, b2, b3]));
    component.loadBudgets();

    expect(component.monthGroups.length).toBe(2);

    const juneGroup = component.monthGroups.find(g => g.month === 5);
    expect(juneGroup?.budgets.length).toBe(2);

    const julyGroup = component.monthGroups.find(g => g.month === 6);
    expect(julyGroup?.budgets.length).toBe(1);
  });

  // ── openCostForm / closeCostForm ─────────────────────────────

  it('openCostForm() should set showCostFormFor and reset costForm', () => {
    component.openCostForm('b1');

    expect(component.showCostFormFor).toBe('b1');
    expect(component.costForm.name).toBe('');
    expect(component.costForm.amount).toBe(0);
  });

  it('closeCostForm() should clear showCostFormFor and reset costForm', () => {
    component.showCostFormFor = 'b1';
    component.costForm = { name: 'Hotel', amount: 200 };

    component.closeCostForm();

    expect(component.showCostFormFor).toBeNull();
    expect(component.costForm.name).toBe('');
    expect(component.costForm.amount).toBe(0);
  });

  // ── openBudgetForm ───────────────────────────────────────────

  it('openBudgetForm() should populate form fields with existing budget data', () => {
    const budget = makeBudget({ id: 'b1', name: 'Rom Trip', amount: 800 });

    component.openBudgetForm(budget);

    expect(component.newBudgetName).toBe('Rom Trip');
    expect(component.newBudgetAmount).toBe(800);
    expect(component.editingBudget).toEqual(budget);
  });

  // ── loadBudgets ──────────────────────────────────────────────

  it('loadBudgets() should call budgetService.getAll() and populate budgets', () => {
    const mockBudgets = [makeBudget({ id: 'b1' }), makeBudget({ id: 'b2' })];
    budgetServiceSpy.getAll.and.returnValue(of(mockBudgets));

    component.loadBudgets();

    expect(component.budgets.length).toBe(2);
    expect(component.isLoading).toBeFalse();
  });

  it('loadBudgets() should set isLoading to false on error', () => {
    budgetServiceSpy.getAll.and.returnValue(throwError(() => new Error('Network error')));

    component.loadBudgets();

    expect(component.isLoading).toBeFalse();
  });

  // ── deleteBudget ─────────────────────────────────────────────

  it('deleteBudget() should call budgetService.delete() when confirmed', () => {
    const budget = makeBudget({ id: 'b1' });
    component.budgets = [budget];
    budgetServiceSpy.delete.and.returnValue(of(undefined));
    budgetServiceSpy.getAll.and.returnValue(of([]));
    spyOn(window, 'confirm').and.returnValue(true);

    // GSAP mocken: onComplete sofort ausführen
    const gsapToSpy = spyOn(gsap, 'to').and.callFake(
      (_target: unknown, vars: gsap.TweenVars) => {
        if (typeof vars['onComplete'] === 'function') {
          (vars['onComplete'] as () => void)();
        }
        return {} as gsap.core.Tween;
      }
    );

    component.deleteBudget('b1');

    expect(budgetServiceSpy.delete).toHaveBeenCalledWith('b1');
    gsapToSpy.calls.reset();
  });

  it('deleteBudget() should NOT call delete when confirm is cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.deleteBudget('b1');

    expect(budgetServiceSpy.delete).not.toHaveBeenCalled();
  });

  // ── deleteCost ───────────────────────────────────────────────

  it('deleteCost() should call costService.deleteCost() with correct id', () => {
    costServiceSpy.deleteCost.and.returnValue(of(undefined));
    budgetServiceSpy.getAll.and.returnValue(of([]));
    spyOn(window, 'confirm').and.returnValue(true);

    component.deleteCost('c1', 'b1');

    expect(costServiceSpy.deleteCost).toHaveBeenCalledWith('c1');
  });

  // ── submitBudget ─────────────────────────────────────────────

  it('submitBudget() should call budgetService.update() when editingBudget is set', () => {
    const budget = makeBudget({ id: 'b1', name: 'Alter Name', amount: 500 });
    component.editingBudget   = budget;
    component.newBudgetName   = 'Neuer Name';
    component.newBudgetAmount = 1200;
    budgetServiceSpy.update.and.returnValue(of({ ...budget, name: 'Neuer Name', amount: 1200 }));
    budgetServiceSpy.getAll.and.returnValue(of([]));

    component.submitBudget();

    expect(budgetServiceSpy.update).toHaveBeenCalledWith('b1', jasmine.objectContaining({
      name: 'Neuer Name',
      amount: 1200
    }));
  });

  it('submitBudget() should do nothing when name or amount is missing', () => {
    component.newBudgetName   = '';
    component.newBudgetAmount = null;

    component.submitBudget();

    expect(budgetServiceSpy.update).not.toHaveBeenCalled();
  });

  // ── saveCost ─────────────────────────────────────────────────

  it('saveCost() should call costService.addCostToBudget() with correct data', () => {
    const budget = makeBudget({ id: 'b1' });
    component.costForm = { name: 'Museum', amount: 15 };
    costServiceSpy.addCostToBudget.and.returnValue(of({ id: 'c1', name: 'Museum', amount: 15, budget }));
    budgetServiceSpy.getAll.and.returnValue(of([]));

    component.saveCost(budget);

    expect(costServiceSpy.addCostToBudget).toHaveBeenCalledWith('b1', jasmine.objectContaining({
      name: 'Museum',
      amount: 15
    }));
  });

  // ── toggleExpand ─────────────────────────────────────────────

  it('toggleExpand() should set expandedBudget when collapsed', () => {
    const budget = makeBudget({ id: 'b1', costs: [] });
    component.expandedBudget = null;

    component.toggleExpand(budget);

    expect(component.expandedBudget as string | null).toEqual('b1');
  });

  it('toggleExpand() should collapse when same budget is clicked again', () => {
    const budget = makeBudget({ id: 'b1' });
    component.expandedBudget = 'b1';

    component.toggleExpand(budget);

    expect(component.expandedBudget).toBeNull();
  });
});
